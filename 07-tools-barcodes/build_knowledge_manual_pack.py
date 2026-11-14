"""Render the repository's Chinese manual as offline HTML and package only its public fixtures."""
from pathlib import Path
import html
import re
from urllib.parse import quote
import zipfile
import shutil

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / '05-docs/课程实训/任务三手工测试包'


def inline(text):
    text = html.escape(text)
    text = re.sub(r'`([^`]+)`', lambda m: ('<a class="file-link" href="' + quote(m[1]) + '" download>' + m[1] + '</a>')
                  if (PACK / m[1]).is_file() else '<code>' + m[1] + '</code>', text)
    return re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)


def render():
    lines = (PACK / '00-按顺序测试.md').read_text(encoding='utf-8').splitlines()
    output = []; index = heading = 0
    while index < len(lines):
        line = lines[index]
        if not line.strip(): index += 1; continue
        if line.startswith('```'):
            code = []; index += 1
            while index < len(lines) and not lines[index].startswith('```'): code.append(lines[index]); index += 1
            output.append('<div class="question"><button type="button" onclick="copyQuestion(this)">复制问题</button><pre>' + html.escape('\n'.join(code)) + '</pre></div>'); index += 1; continue
        if line.startswith('|'):
            rows = []
            while index < len(lines) and lines[index].startswith('|'):
                cells = [p.strip() for p in lines[index].strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', cell) for cell in cells): rows.append(cells)
                index += 1
            output.append('<div class="table-wrap"><table><thead><tr>' + ''.join('<th>' + inline(c) + '</th>' for c in rows[0]) + '</tr></thead><tbody>' + ''.join('<tr>' + ''.join('<td>' + inline(c) + '</td>' for c in row) + '</tr>' for row in rows[1:]) + '</tbody></table></div>'); continue
        match = re.match(r'^(#{1,3}) (.+)', line)
        if match:
            level = len(match[1]); heading += 1
            output.append(f'<h{level} id="section-{heading}">' + inline(match[2]) + f'</h{level}>'); index += 1; continue
        image = re.fullmatch(r'!\[([^]]*)\]\(([^)]+)\)', line)
        if image:
            output.append('<figure><img loading="lazy" src="' + quote(image[2], safe='/') + '" alt="' + html.escape(image[1], quote=True) + '"><figcaption>' + html.escape(image[1]) + '</figcaption></figure>'); index += 1; continue
        if re.match(r'^\d+\. ', line) or line.startswith('- '):
            ordered = bool(re.match(r'^\d+\. ', line)); tag = 'ol' if ordered else 'ul'; output.append('<' + tag + '>')
            while index < len(lines) and (bool(re.match(r'^\d+\. ', lines[index])) if ordered else lines[index].startswith('- ')):
                output.append('<li>' + inline(re.sub(r'^\d+\. |^- ', '', lines[index])) + '</li>'); index += 1
            output.append('</' + tag + '>'); continue
        output.append('<p>' + inline(line) + '</p>'); index += 1
    css = '''body{margin:0;background:#faf9f6;color:#594a37;font:15px/1.85 "Microsoft YaHei",system-ui,sans-serif}main{max-width:1050px;margin:35px auto;padding:32px 42px;background:#fffdf9;border:1px solid #e7dfd2;border-radius:12px}h1{font-size:27px;line-height:1.5}h2{font-size:21px;border-top:1px solid #e7dfd2;padding-top:24px;margin-top:34px}h3{font-size:17px;color:#89613e}p,li{overflow-wrap:anywhere}li{margin:7px 0}.table-wrap{overflow:auto}table{width:100%;border-collapse:collapse;font-size:13px;margin:18px 0}th,td{border:1px solid #e7dfd2;padding:10px 12px;text-align:left;vertical-align:top}th{background:#f4ede1}code{background:#f3ede2;padding:2px 5px;border-radius:4px}.question{background:#f6f1e8;border:1px solid #ddcfbb;border-radius:7px;padding:15px 18px;margin:18px 0}.question button,nav button{border:1px solid #b9a185;background:#89613e;color:white;border-radius:5px;padding:6px 10px;cursor:pointer}.question button{float:right;margin:0 0 8px 10px}pre{white-space:pre-wrap;overflow-wrap:anywhere;margin:0;font:14px/1.8 "Microsoft YaHei",sans-serif}a{color:#89613e}.file-link{font-family:monospace}nav{display:flex;flex-wrap:wrap;gap:10px;align-items:center;padding:15px;background:#f4ede1;border-radius:8px}nav a{text-decoration:none;font-size:13px}strong{color:#69482d}figure{margin:20px 0}figure img{max-width:100%;height:auto;border:1px solid #e7dfd2;border-radius:7px}figcaption{font-size:12px;color:#958875}@media(max-width:720px){main{margin:10px;padding:18px}h1{font-size:22px}th,td{padding:8px;font-size:12px}}@media print{main{border:0;margin:0;padding:0}.question button,nav{display:none}h2,h3{break-after:avoid}table,.question{break-inside:avoid}}'''
    script = '''async function copyQuestion(button){const text=button.parentElement.querySelector('pre').textContent;try{await navigator.clipboard.writeText(text);button.textContent='已复制'}catch{const area=document.createElement('textarea');area.value=text;document.body.appendChild(area);area.select();const ok=document.execCommand('copy');area.remove();button.textContent=ok?'已复制':'请手工选择复制'}setTimeout(()=>button.textContent='复制问题',1800)}'''
    nav = '<nav><a href="#section-1">从头开始</a><a href="' + quote('06_可直接复制的问题.txt') + '" download>下载问题清单</a><a href="http://127.0.0.1:5173/knowledge" target="_blank" rel="noopener">打开知识工作台</a><button type="button" onclick="window.print()">打印或另存为PDF</button></nav>'
    document = '<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>任务三三个区域手工测试流程</title><style>' + css + '</style></head><body><main>' + nav + ''.join(output) + '</main><script>' + script + '</script></body></html>'
    (PACK / '00-打开测试操作指南.html').write_text(document, encoding='utf-8')


def main():
    render()
    target = PACK.with_suffix('.zip')
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for file in sorted(PACK.rglob('*')):
            if file.is_file() and not file.is_symlink():
                archive.write(file, Path(PACK.name) / file.relative_to(PACK))
    public = ROOT / '02-frontend-web/public/knowledge-manual-test-pack.zip'
    public.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(target, public)
    print('Built offline guide and test pack; no runtime configuration included.')


if __name__ == '__main__':
    main()
