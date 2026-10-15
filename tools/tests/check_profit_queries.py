"""Exercise the actual report SELECT strings against SQLite boundary fixtures."""
import re, sqlite3, pathlib, xml.etree.ElementTree as ET
source=pathlib.Path('app/src/main/java/com/electrostore/app/Db.java').read_text()
query=re.search(r'"(SELECT day,total,cost_price,qty FROM sales WHERE day>=\? AND day<\?)"',source).group(1)
con=sqlite3.connect(':memory:')
con.executescript('CREATE TABLE sales(day TEXT,total REAL,cost_price REAL,qty INTEGER); CREATE TABLE service_payments(day TEXT,amount REAL);')
rows=[('2025-12-31',100,50,1),('2026-08-01',700,600,1),('2026-09-30',1400,600,2),('2026-12-31',80,100,1),('2027-01-01',900,600,1),('2028-02-29',30,10,1)]
con.executemany('INSERT INTO sales VALUES (?,?,?,?)',rows)
con.execute("INSERT INTO service_payments VALUES ('2026-09-30',99999)")
def read(a,b):return con.execute(query,(a,b)).fetchall()
assert read('2026-01-01','2027-01-01')==rows[1:4]
assert read('2026-09-01','2026-10-01')==[rows[2]]
assert read('2027-01-01','2028-01-01')==[rows[4]]
assert read('2026-01-01','2026-08-01')==[]
assert read('2028-02-01','2028-03-01')==[rows[5]]
assert read('2026-09-30','2026-10-01')==[rows[2]]
assert sum(r[1]-r[2]*r[3] for r in read('2026-01-01','2027-01-01'))==280
con.execute("DELETE FROM sales WHERE day='2026-09-30'")
assert len(read('2026-01-01','2027-01-01'))==2
print('PASS SQLite: year/month/day boundaries, late recording start, leap day, service exclusion, deletion reflected')
for p in pathlib.Path('app/src/main/res').rglob('*.xml'):ET.parse(p)
layout=ET.parse('app/src/main/res/layout/activity_product_profit.xml')
ns='{http://schemas.android.com/apk/res/android}'
ids={node.get(ns+'id','').split('/')[-1] for node in layout.iter()}
activity=pathlib.Path('app/src/main/java/com/electrostore/app/ProductProfitActivity.java').read_text()
assert set(re.findall(r'R.id.(\w+)',activity))<=ids
assert 'list' not in ids and 'profitMonthGrid' in ids and 'btnThisYear' in ids
assert 'profit_breakdown' not in pathlib.Path('app/src/main/res/layout/activity_product_profit.xml').read_text()
print('PASS XML resources parse, activity IDs resolve, product breakdown removed')
