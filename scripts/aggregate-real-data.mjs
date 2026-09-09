import fs from 'node:fs/promises'

const seedRun = JSON.parse(await fs.readFile('./bgc-measurements-query-seed-raw.json', 'utf8'))
const retryRun = JSON.parse(await fs.readFile('./bgc-retry-failed-lookups-raw.json', 'utf8'))

let allChecks = []
const emp008 = seedRun.q1.find((r) => r.identity.employeeId === 'EMP-008')
if (emp008?.result?.body?.checks) {
  allChecks.push(...emp008.result.body.checks.map((c) => ({ ...c, employeeId: 'EMP-008' })))
}

for (const [empId, attempts] of Object.entries(retryRun)) {
  const success = attempts.find((a) => a.status === 200 && a.body?.checks)
  if (success) {
    allChecks.push(...success.body.checks.map((c) => ({ ...c, employeeId: empId })))
  }
}

console.log('총 레코드 수:', allChecks.length)

const statusCounts = {}
for (const c of allChecks) statusCounts[c.status] = (statusCounts[c.status] || 0) + 1
console.log('상태 분포:', statusCounts)

function pct(arr, p) {
  const idx = Math.min(arr.length - 1, Math.ceil((p / 100) * arr.length) - 1)
  return arr[Math.max(0, idx)]
}

const completionSecs = allChecks
  .filter((c) => c.completedAt)
  .map((c) => (new Date(c.completedAt) - new Date(c.createdAt)) / 1000)
  .sort((a, b) => a - b)

console.log(`전체 완료시간(초) n=${completionSecs.length}`)
console.log(
  `  min=${completionSecs[0].toFixed(2)} p50=${pct(completionSecs, 50).toFixed(2)} p90=${pct(completionSecs, 90).toFixed(2)} p95=${pct(completionSecs, 95).toFixed(2)} p99=${pct(completionSecs, 99).toFixed(2)} max=${completionSecs[completionSecs.length - 1].toFixed(2)}`,
)

const instant = completionSecs.filter((s) => s === 0).length
const delayed = completionSecs.filter((s) => s > 0).sort((a, b) => a - b)
console.log(`즉시완료(0초): ${instant} (${((100 * instant) / completionSecs.length).toFixed(0)}%)`)
console.log(
  `지연완료 n=${delayed.length} p50=${pct(delayed, 50).toFixed(1)} p90=${pct(delayed, 90).toFixed(1)} p95=${pct(delayed, 95).toFixed(1)} p99=${pct(delayed, 99).toFixed(1)} max=${delayed[delayed.length - 1].toFixed(1)}`,
)

const dates = allChecks.map((c) => c.createdAt).sort()
console.log('createdAt 범위:', dates[0], '~', dates[dates.length - 1])

await fs.writeFile('./bgc-aggregated-real-data.json', JSON.stringify({ allChecks, statusCounts, completionSecs }, null, 2))
console.log('\n집계 데이터 저장: ./bgc-aggregated-real-data.json')
