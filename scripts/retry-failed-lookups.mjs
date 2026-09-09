#!/usr/bin/env node
// 1차 조회에서 500/503을 받은 employeeId들을 재시도해, 진짜 데이터가 없는 건지
// 아니면 API 자체의 일시적 불안정성 때문에 못 본 건지 구분한다.

const BASE_URL = process.env.BGC_API_BASE_URL ?? 'https://54capvm12g.execute-api.ap-northeast-2.amazonaws.com'
const ATTEMPTS_PER_ID = 3
const RETRY_DELAY_MS = 3000

const EMPLOYEE_IDS = ['EMP-001', 'EMP-002', 'EMP-003', 'EMP-004', 'EMP-005', 'EMP-006', 'EMP-007', 'EMP-009', 'EMP-010']

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function listByEmployeeId(employeeId) {
  const start = performance.now()
  try {
    const res = await fetch(`${BASE_URL}/background-checks?employeeId=${encodeURIComponent(employeeId)}`)
    const body = await res.json().catch(() => null)
    return { ms: performance.now() - start, status: res.status, body }
  } catch (e) {
    return { ms: performance.now() - start, error: e.message ?? String(e) }
  }
}

async function main() {
  const results = {}
  for (const employeeId of EMPLOYEE_IDS) {
    results[employeeId] = []
    for (let attempt = 1; attempt <= ATTEMPTS_PER_ID; attempt++) {
      const r = await listByEmployeeId(employeeId)
      const summary = r.error
        ? `error: ${r.error}`
        : `status=${r.status} totalCount=${r.body?.totalCount} (${r.ms.toFixed(0)}ms)`
      console.log(`${employeeId} 시도${attempt}/${ATTEMPTS_PER_ID}: ${summary}`)
      results[employeeId].push(r)
      if (r.status === 200 && r.body?.totalCount > 0) {
        console.log(`  -> 실데이터 발견! totalCount=${r.body.totalCount}`)
        break
      }
      if (attempt < ATTEMPTS_PER_ID) await sleep(RETRY_DELAY_MS)
    }
  }

  console.log('\n\n========== 최종 판정 ==========')
  for (const [employeeId, attempts] of Object.entries(results)) {
    const success = attempts.find((a) => a.status === 200)
    if (success) {
      console.log(`${employeeId}: 데이터 있음 (totalCount=${success.body?.totalCount})`)
    } else {
      console.log(`${employeeId}: ${ATTEMPTS_PER_ID}회 모두 실패 — 데이터 없음으로 판단 (마지막 상태: ${attempts[attempts.length - 1].status ?? attempts[attempts.length - 1].error})`)
    }
  }

  const fs = await import('node:fs/promises')
  await fs.writeFile('./bgc-retry-failed-lookups-raw.json', JSON.stringify(results, null, 2))
  console.log('\n원본 저장: ./bgc-retry-failed-lookups-raw.json')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
