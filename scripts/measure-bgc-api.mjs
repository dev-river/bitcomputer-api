#!/usr/bin/env node
// MEASUREMENTS.md 작성을 위한 실측 스크립트.
// 실제 배포된 Background Check API(swagger.yaml 기준)를 호출해 다음을 기록한다:
//   A) 순차 생성+폴링: POST/GET 지연시간 분포, pending -> 최종상태 소요시간   [현재 주석 처리 — 나중에 테스트]
//   B) 동일 employeeId 중복 POST 시 동작                                    [현재 주석 처리 — 나중에 테스트]
//   C) 동시 요청 시 상태코드 분포 변화                                       [현재 주석 처리 — 나중에 테스트]
//   Q) 조회(GET) 기능 시범 테스트 — employeeId로 목록 조회, checkId로 단건 조회, 필수 파라미터 누락 시 에러 응답
//
// 실행: node scripts/measure-bgc-api.mjs
// 결과: 콘솔 요약 + ./bgc-measurements-query-raw.json (원본 데이터, MEASUREMENTS.md 작성 시 참고)

const BASE_URL = process.env.BGC_API_BASE_URL ?? 'https://54capvm12g.execute-api.ap-northeast-2.amazonaws.com'
// const POLL_PROBE_INTERVAL_MS = 2000 // 측정용 폴링 간격 — POST phase 재개 시 사용
// const POLL_HARD_CAP_MS = 120_000 // POST phase 재개 시 사용
// const CONCURRENCY_BATCH_SIZE = 5 // POST phase 재개 시 사용

const COMPOUND_SURNAMES = ['남궁', '황보', '제갈', '선우', '독고', '사공', '서문', '동방']
function splitName(fullName) {
  for (const s of COMPOUND_SURNAMES) {
    if (fullName.startsWith(s) && fullName.length > s.length) {
      return { lastName: s, firstName: fullName.slice(s.length) }
    }
  }
  return { lastName: fullName.slice(0, 1), firstName: fullName.slice(1) }
}

// 실제 우리 앱의 시드 데이터(V2__seed_employees.sql + EmployeeSeedDataInitializer 백필값) 그대로 사용.
// 조회(GET) 테스트는 employeeId만 쓰고 dateOfBirth는 쓰지 않으므로 EMP-007(생년월일 NULL, 의도된 케이스)도 포함 가능.
// 복성 케이스(EMP-003 남궁서준)도 실제 시드 그대로 포함.
const IDENTITIES = [
  { employeeId: 'EMP-001', name: '김민준', dateOfBirth: '1990-03-15' },
  { employeeId: 'EMP-002', name: '김민준', dateOfBirth: '1994-11-02' },
  { employeeId: 'EMP-003', name: '남궁서준', dateOfBirth: '1988-07-21' }, // 복성 케이스
  { employeeId: 'EMP-004', name: '황보라온', dateOfBirth: '1995-02-09' }, // 복성 케이스
  { employeeId: 'EMP-005', name: '김솔', dateOfBirth: '1992-12-30' },
  { employeeId: 'EMP-006', name: '선우진', dateOfBirth: '1991-05-05' }, // 복성 케이스
  { employeeId: 'EMP-007', name: '이서연', dateOfBirth: null }, // 시드상 생년월일 미확인(의도된 케이스)
  { employeeId: 'EMP-008', name: '박민준', dateOfBirth: '1993-08-17' },
  { employeeId: 'EMP-009', name: '최지우', dateOfBirth: '1996-04-03' },
  { employeeId: 'EMP-010', name: '정하윤', dateOfBirth: '1989-10-11' },
]

async function timed(fn) {
  const start = performance.now()
  try {
    const result = await fn()
    return { ms: performance.now() - start, ...result }
  } catch (e) {
    return { ms: performance.now() - start, error: e.message ?? String(e) }
  }
}

// ── POST 관련 함수/phase는 나중에(실제 데이터 생성 테스트 시점) 재개 — 지금은 조회만 시범 테스트 ──
//
// async function postCheck(identity) {
//   const { lastName, firstName } = splitName(identity.name)
//   return timed(async () => {
//     const res = await fetch(`${BASE_URL}/background-checks`, {
//       method: 'POST',
//       headers: { 'Content-Type': 'application/json' },
//       body: JSON.stringify({
//         employeeId: identity.employeeId,
//         firstName,
//         lastName,
//         dateOfBirth: identity.dateOfBirth,
//       }),
//     })
//     const body = await res.json().catch(() => null)
//     return { status: res.status, retryAfter: res.headers.get('retry-after'), body }
//   })
// }
//
// async function phaseA_sequentialCreateAndPoll() { ... }
// async function phaseB_duplicatePost() { ... }
// async function phaseC_concurrency() { ... }

async function getCheck(checkId) {
  return timed(async () => {
    const res = await fetch(`${BASE_URL}/background-checks/${checkId}`)
    const body = await res.json().catch(() => null)
    return { status: res.status, retryAfter: res.headers.get('retry-after'), body }
  })
}

async function listByEmployeeId(employeeId) {
  return timed(async () => {
    const url = employeeId != null
      ? `${BASE_URL}/background-checks?employeeId=${encodeURIComponent(employeeId)}`
      : `${BASE_URL}/background-checks`
    const res = await fetch(url)
    const body = await res.json().catch(() => null)
    return { status: res.status, retryAfter: res.headers.get('retry-after'), body }
  })
}

function percentile(sorted, p) {
  if (sorted.length === 0) return null
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)
  return sorted[Math.max(0, idx)]
}

function summarize(label, values) {
  const sorted = [...values].sort((a, b) => a - b)
  console.log(`\n${label} (n=${sorted.length})`)
  if (sorted.length === 0) {
    console.log('  (표본 없음)')
    return { n: 0 }
  }
  console.log(`  p50=${percentile(sorted, 50)?.toFixed(0)}ms  p95=${percentile(sorted, 95)?.toFixed(0)}ms  p99=${percentile(sorted, 99)?.toFixed(0)}ms  max=${sorted[sorted.length - 1]?.toFixed(0)}ms  min=${sorted[0]?.toFixed(0)}ms`)
  return { n: sorted.length, p50: percentile(sorted, 50), p95: percentile(sorted, 95), p99: percentile(sorted, 99), max: sorted[sorted.length - 1], min: sorted[0] }
}

async function phaseQ1_listByEmployeeId() {
  console.log('\n=== Phase Q1: GET /background-checks?employeeId=X (표본 10명) ===')
  const results = []
  for (const identity of IDENTITIES) {
    const r = await listByEmployeeId(identity.employeeId)
    console.log(`  ${identity.employeeId}: status=${r.status ?? r.error}, totalCount=${r.body?.totalCount}, ${r.ms.toFixed(0)}ms`)
    results.push({ identity, result: r })
  }
  return results
}

async function phaseQ2_missingRequiredParam() {
  console.log('\n=== Phase Q2: employeeId 파라미터 누락 시 응답 (명세상 400) ===')
  const r = await listByEmployeeId(null)
  console.log(`  status=${r.status ?? r.error}, body=${JSON.stringify(r.body)}, ${r.ms.toFixed(0)}ms`)
  return r
}

async function phaseQ3_getSpecificCheck() {
  console.log('\n=== Phase Q3: GET /background-checks/{checkId} 단건 조회 ===')
  // swagger.yaml 예시에 등장하는 checkId — 실존 여부와 무관하게 실제 API의 404/에러 응답 형태를 확인하는 목적
  const exampleFromSpec = 'CHK-a1b2c3d4-e5f6-7890-abcd-ef1234567890'
  const obviouslyFake = 'CHK-does-not-exist-0000'

  const r1 = await getCheck(exampleFromSpec)
  console.log(`  명세 예시 checkId: status=${r1.status ?? r1.error}, ${r1.ms.toFixed(0)}ms, body=${JSON.stringify(r1.body)}`)

  const r2 = await getCheck(obviouslyFake)
  console.log(`  임의 checkId: status=${r2.status ?? r2.error}, ${r2.ms.toFixed(0)}ms, body=${JSON.stringify(r2.body)}`)

  return { exampleFromSpec: r1, obviouslyFake: r2 }
}

async function main() {
  console.log(`Background Check API 조회(GET) 시범 테스트 시작 — base=${BASE_URL}`)
  console.log('(POST 관련 phase는 나중에 테스트 예정이라 이번 실행에서는 제외했습니다)')

  const q1 = await phaseQ1_listByEmployeeId()
  const q2 = await phaseQ2_missingRequiredParam()
  const q3 = await phaseQ3_getSpecificCheck()

  // ── POST 재개 시 여기서 phaseA/B/C를 호출하고, 아래 요약 계산에 POST/폴링 통계를 다시 포함시킬 것 ──
  // const phaseA = await phaseA_sequentialCreateAndPoll()
  // const phaseB = await phaseB_duplicatePost()
  // const phaseC = await phaseC_concurrency()

  const listLatencies = q1.map((r) => r.result.ms)

  console.log('\n\n========== 요약 ==========')
  const listSummary = summarize('GET /background-checks?employeeId= 지연시간', listLatencies)
  console.log(`\n필수 파라미터 누락 응답: status=${q2.status ?? q2.error}`)
  console.log(`단건 조회(404 등) 응답: 예시ID=${q3.exampleFromSpec.status ?? q3.exampleFromSpec.error}, 임의ID=${q3.obviouslyFake.status ?? q3.obviouslyFake.error}`)

  const raw = { baseUrl: BASE_URL, generatedAt: new Date().toISOString(), q1, q2, q3, listSummary }
  const fs = await import('node:fs/promises')
  await fs.writeFile('./bgc-measurements-query-seed-raw.json', JSON.stringify(raw, null, 2))
  console.log('\n원본 데이터 저장: ./bgc-measurements-query-seed-raw.json')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
