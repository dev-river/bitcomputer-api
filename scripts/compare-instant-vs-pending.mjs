#!/usr/bin/env node
// EMP-005(생성 시점에 즉시 clear로 온 "일반 케이스")와 EMP-006(생성 시점에 pending으로 온 케이스)을
// 비교 실측한다. EMP-006은 최종상태(clear/flagged)로 바뀔 때까지 실제로 폴링해서 걸린 시간을 잰다.

const BASE_URL = process.env.BGC_API_BASE_URL ?? 'https://54capvm12g.execute-api.ap-northeast-2.amazonaws.com'
const POLL_INTERVAL_MS = 3000 // 실측용 촘촘한 폴링 간격(운영값 bgc.poll-interval-ms와는 별개)
const POLL_HARD_CAP_MS = 10 * 60 * 1000 // 10분 넘게 안 끝나면 포기(스크립트 안전장치)

// post-seed-test.mjs 실행 결과에서 가져온 실제 값
const NORMAL_CASE = {
  employeeId: 'EMP-005',
  checkId: 'CHK-514b4089-7cf4-415a-a9e3-d15d92cb0d53',
  createdAt: '2026-09-08T03:34:23.090Z',
  statusAtCreation: 'clear',
}
const PENDING_CASE = {
  employeeId: 'EMP-006',
  checkId: 'CHK-b5cb1d48-e703-4f25-b314-ab12042df8ee',
  createdAt: '2026-09-08T03:34:24.142Z',
  statusAtCreation: 'pending',
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function getCheck(checkId) {
  const start = performance.now()
  try {
    const res = await fetch(`${BASE_URL}/background-checks/${checkId}`)
    const body = await res.json().catch(() => null)
    return { ms: performance.now() - start, status: res.status, body }
  } catch (e) {
    return { ms: performance.now() - start, error: e.message ?? String(e) }
  }
}

async function main() {
  console.log('=== 일반 케이스 (EMP-005) — 생성 시점에 이미 최종상태였음 ===')
  const normalGet = await getCheck(NORMAL_CASE.checkId)
  console.log(`  현재 조회: status=${normalGet.status ?? normalGet.error}, body=${JSON.stringify(normalGet.body)}`)
  const normalElapsedSec = normalGet.body?.completedAt
    ? (new Date(normalGet.body.completedAt) - new Date(NORMAL_CASE.createdAt)) / 1000
    : 0
  console.log(`  createdAt=${NORMAL_CASE.createdAt} completedAt=${normalGet.body?.completedAt ?? '(POST 응답 시점에 이미 확정)'}`)
  console.log(`  => 생성부터 확정까지: ${normalElapsedSec.toFixed(2)}초 (사실상 0초, 폴링 불필요)\n`)

  console.log('=== Pending 케이스 (EMP-006) — 실시간 폴링 시작 ===')
  const pollLog = []
  const pollStart = performance.now()
  let finalStatus = PENDING_CASE.statusAtCreation
  let finalBody = null

  while (performance.now() - pollStart < POLL_HARD_CAP_MS) {
    await sleep(POLL_INTERVAL_MS)
    const r = await getCheck(PENDING_CASE.checkId)
    const elapsedSec = (performance.now() - pollStart) / 1000
    pollLog.push({ elapsedSec, ...r })
    console.log(`  [+${elapsedSec.toFixed(1)}s] status=${r.status ?? r.error}, bgcStatus=${r.body?.status}, 응답지연=${r.ms.toFixed(0)}ms`)
    if (r.body?.status && r.body.status !== 'pending') {
      finalStatus = r.body.status
      finalBody = r.body
      break
    }
  }

  const totalElapsedMs = performance.now() - pollStart
  console.log(`\n최종상태: ${finalStatus}`)
  if (finalBody) {
    const wallClockSec = (new Date(finalBody.completedAt) - new Date(PENDING_CASE.createdAt)) / 1000
    console.log(`  API 기준 생성->완료: ${wallClockSec.toFixed(2)}초 (createdAt=${PENDING_CASE.createdAt}, completedAt=${finalBody.completedAt})`)
    console.log(`  우리 스크립트가 실제로 폴링 시작~완료 감지까지 걸린 시간: ${(totalElapsedMs / 1000).toFixed(2)}초 (폴링 간격 ${POLL_INTERVAL_MS}ms이므로 최대 그만큼의 감지 지연 포함)`)
  } else {
    console.log(`  ${POLL_HARD_CAP_MS / 1000}초 안에 완료되지 않음 — 여전히 pending`)
  }

  console.log('\n\n========== 비교 요약 ==========')
  console.log(`일반 케이스(EMP-005): 생성 즉시 확정, 0초 (폴링 불필요)`)
  console.log(`Pending 케이스(EMP-006): 확정까지 약 ${finalBody ? ((new Date(finalBody.completedAt) - new Date(PENDING_CASE.createdAt)) / 1000).toFixed(1) : '미완료'}초 소요, 폴링 ${pollLog.length}회 필요`)

  const fs = await import('node:fs/promises')
  await fs.writeFile(
    './bgc-instant-vs-pending-raw.json',
    JSON.stringify({ normalCase: { ...NORMAL_CASE, get: normalGet, elapsedSec: normalElapsedSec }, pendingCase: { ...PENDING_CASE, pollLog, finalStatus, finalBody } }, null, 2),
  )
  console.log('\n원본 데이터 저장: ./bgc-instant-vs-pending-raw.json')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
