"""Explicit opt-in acceptance of the integrated original-file/vector/graph pipeline.
Only creates and removes UUID-named test documents; credentials/tokens never enter the report.
"""
import argparse
import json
import os
from pathlib import Path
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-live', action='store_true')
    parser.add_argument('--base-url', default='http://127.0.0.1:5173/api')
    parser.add_argument('--user', default='109001')
    args = parser.parse_args()
    if not args.run_live:
        parser.error('--run-live is required for actual model calls and temporary file writes')
    token = None
    prefix = 'joint-live-' + uuid.uuid4().hex[:12]
    ids = [prefix + '-process', prefix + '-equipment']
    checks, cleanup, failure = [], [], None

    def call(path, body=None, method=None, content_type='application/json', raw=False):
        headers = {'Content-Type': content_type}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        data = body if isinstance(body, bytes) else None if body is None else json.dumps(body, ensure_ascii=False).encode('utf-8')
        request = urllib.request.Request(args.base_url.rstrip('/') + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=1000) as response:
                return response.read() if raw else json.load(response)
        except urllib.error.HTTPError as error:
            return json.load(error)

    def step(name, action, validate):
        started = time.monotonic()
        response = action()
        passed = bool(validate(response))
        checks.append({'name': name, 'passed': passed, 'seconds': round(time.monotonic() - started, 2),
                       'response': response if not isinstance(response, bytes) else {'downloadedBytes': len(response)}})
        print(('PASS ' if passed else 'FAIL ') + name, flush=True)
        if not passed:
            raise AssertionError(name + ': ' + str(response.get('msg', response.get('data', 'unexpected result'))) if isinstance(response, dict) else name)
        return response

    def upload(index, version, text):
        boundary = 'MouseMES' + uuid.uuid4().hex
        sections = []
        for name, value in {'documentId': ids[index], 'version': str(version), 'title': '联合真实验收示例 ' + str(index + 1)}.items():
            sections.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode('utf-8'))
        sections.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="sop.txt"\r\nContent-Type: text/plain\r\n\r\n'.encode())
        sections.extend([text.encode('utf-8'), f'\r\n--{boundary}--\r\n'.encode()])
        return call('/rag/documents', b''.join(sections), 'POST', 'multipart/form-data; boundary=' + boundary)

    def ask(mode):
        return call('/knowledge/messages', {'question': '联合验收工序使用的设备由谁负责？', 'mode': mode, 'documentIds': ids})

    try:
        login = call('/auth/login', {'userCode': args.user, 'password': os.environ.get('MES_VERIFY_PASSWORD', '123456')})
        token = login.get('data', {}).get('token')
        if not token:
            raise RuntimeError('Login failed')
        step('加载联合版本及真实向量检索器', lambda: call('/knowledge/status?probe=true'), lambda r: r.get('code') == 200 and r['data'].get('revision') == 'task2-task3-r1' and r['data'].get('vectorRetrieverConnected') and r['data'].get('neo4j') == '已连接')
        step('嵌入模型已配置', lambda: call('/rag/status'), lambda r: r.get('code') == 200 and r['data'].get('embeddingConfigured') and r['data'].get('graphAvailable'))
        a = '本文件为课程验收虚构资料。联合验收工序使用联合验收设备。'
        b = '本文件为课程验收虚构资料。联合验收设备由联合负责人甲负责。'
        c = '本文件为课程验收虚构资料。联合验收设备由联合负责人乙负责。'
        ready = lambda r: r.get('code') == 200 and r['data'].get('status') == 'READY' and r['data'].get('graphStatus') == 'READY'
        step('工序原件与两类索引共同发布', lambda: upload(0, 1, a), ready)
        step('设备原件与两类索引共同发布', lambda: upload(1, 1, b), ready)
        step('下载原件字节一致', lambda: call('/rag/documents/' + ids[1] + '/download', raw=True), lambda r: r == b.encode('utf-8'))
        chunk_ids = {}
        for mode in ['GRAPHRAG', 'RAG', 'HYBRID']:
            response = step(mode + '真实回答引用共同版本', lambda mode=mode: ask(mode), lambda r: r.get('code') == 200 and '联合负责人甲' in r['data'].get('answer', '') and bool(r['data'].get('sources')))
            for source in response['data']['sources']:
                previous = chunk_ids.setdefault(source['documentId'], source['chunkId'])
                if previous != source['chunkId']:
                    raise AssertionError('Modes used inconsistent chunk IDs')
        step('替换原件并同步发布第二版本', lambda: upload(1, 2, c), ready)
        step('向量问答仅返回新负责人', lambda: ask('RAG'), lambda r: r.get('code') == 200 and '联合负责人乙' in r['data'].get('answer', '') and '联合负责人甲' not in r['data']['answer'] and all(s['version'] == 2 for s in r['data'].get('sources', []) if s['documentId'] == ids[1]))
        step('删除撤销两类索引并完成清理', lambda: call('/rag/documents/' + ids[1] + '?version=3', method='DELETE'), lambda r: r.get('code') == 200 and not r['data'].get('cleanupPending') and not r['data'].get('graphCleanupPending'))
        step('删除文件不能再被指定检索', lambda: ask('HYBRID'), lambda r: r.get('code') == 403)
    except Exception as error:
        failure = str(error)
        print(failure, file=sys.stderr)
    finally:
        if token:
            try:
                for doc in call('/rag/documents').get('data', []):
                    if doc['documentId'] in ids:
                        cleanup.append(call('/rag/documents/' + doc['documentId'] + '?version=' + str(doc['version'] + 1), method='DELETE'))
                for event in call('/rag/events').get('data', []):
                    if event['documentId'] in ids and event['action'] == 'DELETE' and (event['status'] == 'CLEANUP_PENDING' or event.get('graphStatus') == 'CLEANUP_PENDING'):
                        cleanup.append(call('/rag/documents/' + event['documentId'] + '/retry?version=' + str(event['version']), {}, 'POST'))
                remaining = [d for d in call('/rag/documents').get('data', []) if d['documentId'] in ids]
                if remaining or any(r.get('code') != 200 or r.get('data', {}).get('cleanupPending') or r.get('data', {}).get('graphCleanupPending') for r in cleanup):
                    failure = (failure or '') + '; temporary document cleanup incomplete'
            except Exception as error:
                failure = (failure or '') + '; cleanup: ' + str(error)
        output = Path('.runtime/joint-knowledge-live/acceptance.json')
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps({'realModel': True, 'documentIds': ids, 'checks': checks, 'cleanup': cleanup, 'success': failure is None, 'failure': failure}, ensure_ascii=False, indent=2), encoding='utf-8')
    return 0 if failure is None else 1


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')
    raise SystemExit(main())
