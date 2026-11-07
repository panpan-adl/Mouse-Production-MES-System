"""Opt-in real-model course acceptance: PDF/DOCX, three documents, two sessions and UI evidence.
Only UUID-named fictional fixtures are created; tokens/keys are never written to the report.
"""
import argparse
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.request
import urllib.error
import uuid
import zipfile
from xml.sax.saxutils import escape


def pdf(pages):
    objects = [b'<< /Type /Catalog /Pages 2 0 R >>',
               ('<< /Type /Pages /Kids [' + ' '.join(f'{4+i*2} 0 R' for i in range(len(pages))) + f'] /Count {len(pages)} >>').encode(),
               b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>']
    for index, text in enumerate(pages):
        stream = ('BT /F1 11 Tf 40 740 Td (' + text.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)') + ') Tj ET').encode('ascii')
        objects.extend([f'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 900 800] /Resources << /Font << /F1 3 0 R >> >> /Contents {5+index*2} 0 R >>'.encode(),
                        f'<< /Length {len(stream)} >>\nstream\n'.encode() + stream + b'\nendstream'])
    output = bytearray(b'%PDF-1.4\n'); offsets = [0]
    for index, obj in enumerate(objects, 1):
        offsets.append(len(output)); output.extend(f'{index} 0 obj\n'.encode() + obj + b'\nendobj\n')
    start = len(output); output.extend(f'xref\n0 {len(offsets)}\n0000000000 65535 f \n'.encode())
    for offset in offsets[1:]: output.extend(f'{offset:010d} 00000 n \n'.encode())
    output.extend(f'trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{start}\n%%EOF'.encode())
    return bytes(output)


def docx(paragraphs):
    output = io.BytesIO()
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('[Content_Types].xml', '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>')
        archive.writestr('_rels/.rels', '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>')
        archive.writestr('word/document.xml', '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>' + ''.join('<w:p><w:r><w:t>' + escape(p) + '</w:t></w:r></w:p>' for p in paragraphs) + '</w:body></w:document>')
    return output.getvalue()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-live', action='store_true'); parser.add_argument('--capture-ui', action='store_true')
    parser.add_argument('--base-url', default='http://127.0.0.1:5173/api')
    parser.add_argument('--user', default='109001'); parser.add_argument('--other-user', default='108001')
    args = parser.parse_args()
    if not args.run_live: parser.error('--run-live required: this consumes real model calls')
    ids = ['course-' + uuid.uuid4().hex[:16] + '-' + kind for kind in ['pdf', 'docx', 'txt']]
    out = Path('.runtime/task3-final-live'); out.mkdir(parents=True, exist_ok=True)
    checks, comparison, cleanup = [], [], []; cases = {}; failure = None; identity = None

    def call(path, body=None, method=None, token=None, content_type='application/json', raw=False):
        headers = {'Content-Type': content_type}
        if token: headers['Authorization'] = 'Bearer ' + token
        payload = body if isinstance(body, bytes) else None if body is None else json.dumps(body, ensure_ascii=False).encode()
        request = urllib.request.Request(args.base_url.rstrip('/') + path, payload, headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=1000) as response: return response.read() if raw else json.load(response)
        except urllib.error.HTTPError as error: return json.load(error)

    def check(name, valid):
        checks.append({'name': name, 'passed': bool(valid)}); print(('PASS ' if valid else 'FAIL ') + name, flush=True)
        if not valid: raise AssertionError(name)

    def upload(index, filename, data):
        boundary = 'MouseMES' + uuid.uuid4().hex
        content = b''
        for name, value in {'documentId': ids[index], 'version': '1', 'title': '课程联合验收 ' + filename}.items():
            content += f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode()
        content += f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: application/octet-stream\r\n\r\n'.encode() + data + f'\r\n--{boundary}--\r\n'.encode()
        return call('/rag/documents', content, 'POST', token, 'multipart/form-data; boundary=' + boundary)

    question = 'Which equipment does Aurora Process use, who maintains that equipment, and what inspection does this process require?'
    try:
        identity = call('/auth/login', {'userCode': args.user, 'password': os.getenv('MES_VERIFY_PASSWORD', '123456')}).get('data') or {}
        token = identity.get('token'); check('主账号登录', token)
        other = call('/auth/login', {'userCode': args.other_user, 'password': os.getenv('MES_VERIFY_OTHER_PASSWORD', '123456')}).get('data') or {}
        check('第二个不同账号登录', other.get('token') and identity.get('id') != other.get('id'))
        originals = [pdf(['Fictional course demonstration.', 'Aurora Process uses Aurora Robot.']),
                     docx(['Fictional course demonstration.', 'Aurora Robot is maintained by Alice Chen.']),
                     b'Aurora Process requires Daily Check inspection.']
        for index, filename in enumerate(['process.pdf', 'equipment.docx', 'inspection.txt']):
            (out / filename).write_bytes(originals[index])
            result = upload(index, filename, originals[index]); data = result.get('data') or {}
            check(filename + '向量和图谱共同发布' + (('：' + str(data.get('error') or result.get('msg'))) if data.get('status') != 'READY' else ''), result.get('code') == 200 and data.get('status') == 'READY' and data.get('graphStatus') == 'READY')
        for index in [0, 1]:
            check(['PDF', 'DOCX'][index] + '下载原件字节一致', call('/rag/documents/' + ids[index] + '/download', token=token, raw=True) == originals[index])
        graph = call('/knowledge/graph?' + '&'.join('documentIds=' + id for id in ids), token=token).get('data') or {}
        sources = graph.get('sources', [])
        check('图谱来源保留PDF第二页及DOCX第二段', any(s['documentId'] == ids[0] and '第2页' in s.get('location', '') for s in sources) and any(s['documentId'] == ids[1] and '段落2' in s.get('location', '') for s in sources))
        check('文件阶段已持久化为READY', all(d.get('stage') == 'READY' for d in call('/rag/documents', token=token).get('data', []) if d['documentId'] in ids))
        foreign = other['token']
        check('第二账号列表不包含测试资料', not any(d['documentId'] in ids for d in call('/rag/documents', token=foreign).get('data', [])))
        for label, path, body, method in [
            ('详情', '/rag/documents/' + ids[0], None, 'GET'), ('原件下载', '/rag/documents/' + ids[0] + '/download', None, 'GET'),
            ('重试', '/rag/documents/' + ids[0] + '/retry?version=1', {}, 'POST'), ('删除', '/rag/documents/' + ids[0] + '?version=2', None, 'DELETE'),
            ('图谱范围', '/knowledge/graph?documentIds=' + ids[0], None, 'GET'),
            ('问答范围', '/knowledge/messages', {'question': question, 'mode': 'HYBRID', 'documentIds': ids}, 'POST')]:
            check('跨账号拒绝' + label, call(path, body, method, foreign).get('code') == 403)
        shared = {}
        for mode in ['RAG', 'GRAPHRAG', 'HYBRID']:
            started = time.monotonic(); response = call('/knowledge/messages', {'question': question, 'mode': mode, 'documentIds': ids}, token=token); result = response.get('data') or {}
            comparison.append({'mode': mode, 'seconds': round(time.monotonic() - started, 2), 'answer': result.get('answer'), 'sources': result.get('sources', []), 'facts': result.get('facts', [])})
            if mode == 'RAG':
                # A complex multi-document question can legitimately exceed vector recall.
                # Preserve the actual difference rather than requiring identical answers.
                check('RAG独立对照：限定资料范围并返回有引用回答或明确拒答', response.get('code') == 200 and set(s['documentId'] for s in result.get('sources', [])).issubset(set(ids)) and (result.get('answer') == '当前资料不足，无法回答该问题。' or '[S' in result.get('answer', '')))
            else:
                check(mode + '跨三文件回答并引用三份资料', response.get('code') == 200 and all(text in result.get('answer', '') for text in ['Aurora Robot', 'Alice Chen', 'Daily Check']) and set(s['documentId'] for s in result.get('sources', [])) == set(ids))
            for source in result['sources']: check_id = shared.setdefault(source['documentId'], source['chunkId']); assert check_id == source['chunkId']
        alias = call('/knowledge/messages', {'question': 'Aurora机器人由谁维护？', 'mode': 'GRAPHRAG', 'documentIds': ids}, token=token)
        cases['alias']=alias
        check('同义表达定位实体并回答', alias.get('code') == 200 and 'Alice Chen' in (alias.get('data') or {}).get('answer', ''))
        missing = call('/knowledge/messages', {'question': '这些资料记载的2028年度净利润是多少？', 'mode': 'HYBRID', 'documentIds': ids}, token=token)
        cases['insufficient']=missing
        check('资料不足不编造答案', missing.get('code') == 200 and (missing.get('data') or {}).get('answer') == '当前资料不足，无法回答该问题。')
        if args.capture_ui:
            env = os.environ.copy(); env['MES_CAPTURE_AUTH'] = json.dumps(identity); env['MES_CAPTURE_QUESTION'] = question
            env['MES_CAPTURE_DOCUMENT_IDS'] = json.dumps(ids)
            result = subprocess.run([os.getenv('MES_NODE_EXE', r'D:\software\VUE\node.exe'), '07-tools-barcodes/capture_task3_live.cjs'], env=env, timeout=180, check=False)
            check('真实浏览器图谱、回答子图和手机截图', result.returncode == 0)
    except Exception as error:
        failure = str(error); print(failure, file=sys.stderr)
    finally:
        if identity and identity.get('token'):
            try:
                for document in call('/rag/documents', token=identity['token']).get('data', []):
                    if document['documentId'] in ids:
                        response = call('/rag/documents/' + document['documentId'] + '?version=' + str(document['version'] + 1), method='DELETE', token=identity['token'])
                        data = response.get('data') or {}; cleanup.append({'id': document['documentId'], 'success': response.get('code') == 200 and not data.get('cleanupPending') and not data.get('graphCleanupPending')})
                if not all(item['success'] for item in cleanup): failure = (failure or '') + '; fixture cleanup incomplete'
            except Exception as error: failure = (failure or '') + '; cleanup: ' + str(error)
        (out / 'acceptance.json').write_text(json.dumps({'checks': checks, 'comparison': comparison, 'edgeCases': cases, 'cleanup': cleanup, 'success': failure is None, 'failure': failure}, ensure_ascii=False, indent=2), encoding='utf-8')
    return 0 if failure is None else 1


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8'); sys.stderr.reconfigure(encoding='utf-8')
    raise SystemExit(main())
