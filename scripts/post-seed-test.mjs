#!/usr/bin/env node
// 시드 직원 10명(EMP-001~010) 그대로 POST /background-checks 를 보내보고,
// 어떤 ID가 "정상 응답"(201, checkId 발급)을 받는지 확인한다.
// 완료까지 폴링은 하지 않음(별도 단계) — 여기서는 생성 시점 응답만 본다.
//
// 실행: node scripts/post-seed-test.mjs
// 결과: 콘솔 요약 + ./bgc-post-seed-raw.json

const BASE_URL = process.env.BGC_API_BASE_URL ?? 'https://54capvm12g.execute-api.ap-northeast-2.amazonaws.com'
const DELAY_BETWEEN_POSTS_MS = 1000 // 연속 POST가 서로의 지연시간 측정에 영향 안 주도록 약간의 간격
const MAX_ATTEMPTS_ON_5XX = 3 // GET 실측에서 이 API가 유독 500/503이 잦았던 걸 감안 — 4xx(진짜 검증 실패)는 재시도하지 않고, 5xx만 재시도해서 "진짜 거부"와 "일시적 불안정"을 구분
const RETRY_DELAY_MS = 3000

const COMPOUND_SURNAMES = ['남궁', '황보', '제갈', '선우', '독고', '사공', '서문', '동방']
function splitName(fullName) {
  for (const s of COMPOUND_SURNAMES) {
    if (fullName.startsWith(s) && fullName.length > s.length) {
      return { lastName: s, firstName: fullName.slice(s.length) }
    }
  }
  return { lastName: fullName.slice(0, 1), firstName: fullName.slice(1) }
}

// 실제 시드 데이터 그대로 (V2__seed_employees.sql + EmployeeSeedDataInitializer 백필값).
// EMP-007은 시드상 date_of_birth가 NULL인 의도된 케이스 — 명세상 필수 필드 누락이라 400이 기대됨.
const IDENTITIES = [
  { employeeId: 'EMP-001', name: '김민준', dateOfBirth: '1990-03-15' },
  { employeeId: 'EMP-002', name: '김민준', dateOfBirth: '1994-11-02' },
  { employeeId: 'EMP-003', name: '남궁서준', dateOfBirth: '1988-07-21' },
  { employeeId: 'EMP-004', name: '황보라온', dateOfBirth: '1995-02-09' },
  { employeeId: 'EMP-005', name: '김솔', dateOfBirth: '1992-12-30' },
  { employeeId: 'EMP-006', name: '선우진', dateOfBirth: '1991-05-05' },
  { employeeId: 'EMP-007', name: '이서연', dateOfBirth: null },
  { employeeId: 'EMP-008', name: '박민준', dateOfBirth: '1993-08-17' },
  { employeeId: 'EMP-009', name: '최지우', dateOfBirth: '1996-04-03' },
  { employeeId: 'EMP-010', name: '정하윤', dateOfBirth: '1989-10-11' },
]

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function postCheck(identity) {
  const { lastName, firstName } = splitName(identity.name)
  const start = performance.now()
  try {
    const res = await fetch(`${BASE_URL}/background-checks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        employeeId: identity.employeeId,
        firstName,
        lastName,
        dateOfBirth: identity.dateOfBirth,
      }),
    })
    const body = await res.json().catch(() => null)
    return { ms: performance.now() - start, status: res.status, retryAfter: res.headers.get('retry-after'), body, firstName, lastName }
  } catch (e) {
    return { ms: performance.now() - start, error: e.message ?? String(e), firstName, lastName }
  }
}

async function main() {
  console.log(`Background Check API POST 시범 테스트 — base=${BASE_URL}`)
  console.log(`시드 직원 ${IDENTITIES.length}명 순차 발송 (요청 간 ${DELAY_BETWEEN_POSTS_MS}ms 간격)\n`)

  const results = []
  for (const identity of IDENTITIES) {
    const attempts = []
    let r
    for (let attempt = 1; attempt <= MAX_ATTEMPTS_ON_5XX; attempt++) {
      r = await postCheck(identity)
      attempts.push(r)
      const ok = r.status === 201
      const line = r.error
        ? `실패(네트워크): ${r.error}`
        : `status=${r.status}${ok ? ` checkId=${r.body?.checkId} bgcStatus=${r.body?.status} estimatedCompletionSeconds=${r.body?.estimatedCompletionSeconds}` : ` body=${JSON.stringify(r.body)}`}`
      console.log(`${identity.employeeId} 시도${attempt}/${MAX_ATTEMPTS_ON_5XX} (${identity.name} -> lastName="${r.lastName}" firstName="${r.firstName}", dob=${identity.dateOfBirth ?? 'NULL'}): ${line} (${r.ms.toFixed(0)}ms)`)
      const isServerError = r.status === 500 || r.status === 503 || r.error
      if (ok || !isServerError || attempt === MAX_ATTEMPTS_ON_5XX) break
      await sleep(RETRY_DELAY_MS)
    }
    results.push({ identity, result: r, attempts })
    await sleep(DELAY_BETWEEN_POSTS_MS)
  }

  console.log('\n\n========== 요약 ==========')
  const normal = results.filter((r) => r.result.status === 201)
  const abnormal = results.filter((r) => r.result.status !== 201)
  console.log(`정상 응답(201) 받은 ID: ${normal.map((r) => r.identity.employeeId).join(', ') || '없음'} (${normal.length}/${results.length})`)
  console.log(`비정상 응답 ID: ${abnormal.map((r) => `${r.identity.employeeId}(${r.result.status ?? r.result.error})`).join(', ') || '없음'}`)

  const fs = await import('node:fs/promises')
  await fs.writeFile('./bgc-post-seed-raw.json', JSON.stringify({ baseUrl: BASE_URL, generatedAt: new Date().toISOString(), results }, null, 2))
  console.log('\n원본 데이터 저장: ./bgc-post-seed-raw.json')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
