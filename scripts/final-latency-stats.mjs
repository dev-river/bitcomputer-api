import fs from 'node:fs/promises'

const run1 = JSON.parse(await fs.readFile('./bgc-measurements-query-raw.json', 'utf8'))
const run2 = JSON.parse(await fs.readFile('./bgc-measurements-query-seed-raw.json', 'utf8'))
const retry = JSON.parse(await fs.readFile('./bgc-retry-failed-lookups-raw.json', 'utf8'))

const allLatencies = []
for (const run of [run1, run2]) {
  for (const r of run.q1) allLatencies.push(r.result.ms)
  allLatencies.push(run.q2.ms)
  allLatencies.push(run.q3.exampleFromSpec.ms)
  allLatencies.push(run.q3.obviouslyFake.ms)
}
for (const attempts of Object.values(retry)) {
  for (const a of attempts) if (a.ms != null) allLatencies.push(a.ms)
}

allLatencies.sort((a, b) => a - b)
function pct(arr, p) {
  const idx = Math.min(arr.length - 1, Math.ceil((p / 100) * arr.length) - 1)
  return arr[Math.max(0, idx)]
}

console.log(`전체 개별 호출(GET 계열) 지연시간 n=${allLatencies.length}`)
console.log(
  `  min=${allLatencies[0].toFixed(0)}ms p50=${pct(allLatencies, 50).toFixed(0)}ms p90=${pct(allLatencies, 90).toFixed(0)}ms p95=${pct(allLatencies, 95).toFixed(0)}ms p99=${pct(allLatencies, 99).toFixed(0)}ms max=${allLatencies[allLatencies.length - 1].toFixed(0)}ms`,
)

const under5s = allLatencies.filter((v) => v < 5000)
console.log(`\n5초 미만 응답만: n=${under5s.length} (${((100 * under5s.length) / allLatencies.length).toFixed(0)}%)`)
console.log(`  p50=${pct(under5s, 50).toFixed(0)}ms p95=${pct(under5s, 95).toFixed(0)}ms max=${under5s[under5s.length - 1].toFixed(0)}ms`)

const over25s = allLatencies.filter((v) => v > 25000)
console.log(`\n25초 초과(사실상 게이트웨이 타임아웃) 응답: n=${over25s.length} (${((100 * over25s.length) / allLatencies.length).toFixed(0)}%)`)

// 에러율(500/503) 계산
let total = 0
let errorCount = 0
for (const run of [run1, run2]) {
  for (const r of run.q1) {
    total++
    if (r.result.status === 500 || r.result.status === 503) errorCount++
  }
}
for (const attempts of Object.values(retry)) {
  for (const a of attempts) {
    total++
    if (a.status === 500 || a.status === 503) errorCount++
  }
}
console.log(`\nGET 요청 중 500/503 비율: ${errorCount}/${total} (${((100 * errorCount) / total).toFixed(0)}%)`)
