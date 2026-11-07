"""Opt-in real API acceptance. Creates uniquely named fixtures and removes only those fixtures.
Run from the repository root. Model requests use the already-configured backend; no API key is read.
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
    parser.add_argument('--run-live', action='store_true', help='Explicitly enable real model requests and test document writes')
    parser.add_argument('--base-url', default='http://127.0.0.1:5173/api')
    parser.add_argument('--user', default='109001')
    parser.add_argument('--output', default='.runtime/knowledge-live/lifecycle-acceptance.json')
    args = parser.parse_args()
    if not args.run_live:
        parser.error('--run-live is required because this test calls the configured real model')
    token = None
    results = []
    prefix = 'course-check-' + uuid.uuid4().hex[:12]
    ids = [prefix + '-process', prefix + '-equipment']

    def call(path, body=None, method=None):
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        req = urllib.request.Request(args.base_url.rstrip('/') + path,
                                     data=None if body is None else json.dumps(body, ensure_ascii=False).encode('utf-8'),
                                     headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=600) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            return json.load(error)

    def step(name, action, check):
        started = time.monotonic()
        response = action()
        passed = bool(check(response))
        results.append({'name': name, 'passed': passed, 'seconds': round(time.monotonic() - started, 2), 'response': response})
        print(('PASS ' if passed else 'FAIL ') + name, flush=True)
        if not passed:
            raise AssertionError(name + ': ' + str(response.get('msg', 'unexpected result')))
        return response.get('data')

    def upload(index, version, text):
        return call('/knowledge/documents', {'documentId': ids[index], 'title': '临时生命周期验收资料 ' + str(index + 1), 'version': version, 'text': text})

    def graph():
        return call('/knowledge/graph?' + urllib.parse.urlencode({'documentIds': ids}, doseq=True))

    def ask(mode, scope=None):
        return call('/knowledge/messages', {'question': '验收工序使用的设备由谁负责？', 'mode': mode, 'documentIds': scope or ids})

    def answer_has(name):
        return lambda r: r.get('code') == 200 and name in r.get('data', {}).get('answer', '') and bool(r.get('data', {}).get('sources'))

    success = False
    failure = None
    cleanup = []
    try:
        login = call('/auth/login', {'userCode': args.user, 'password': os.environ.get('MES_VERIFY_PASSWORD', '123456')})
        token = login.get('data', {}).get('token')
        if not token:
            raise RuntimeError('Login failed: ' + str(login.get('msg')))
        step('Neo4j与模型配置有效', lambda: call('/knowledge/status?probe=true'), lambda r: r.get('code') == 200 and r['data'].get('enabled') and r['data'].get('modelConfigured') and r['data'].get('neo4j') == '已连接')
        text_a = '本文件为课程验收虚构资料。验收工序使用验收点胶机。'
        text_b = '本文件为课程验收虚构资料。验收点胶机由验收负责人甲负责。'
        text_c = '本文件为课程验收虚构资料。验收点胶机由验收负责人乙负责。'
        step('真实模型构建工序图谱', lambda: upload(0, 1, text_a), lambda r: r.get('code') == 200 and r['data'].get('relationCount', 0) > 0)
        step('真实模型构建设备图谱', lambda: upload(1, 1, text_b), lambda r: r.get('code') == 200 and r['data'].get('relationCount', 0) > 0)
        step('相同版本重试不重复建图', lambda: upload(1, 1, text_b), lambda r: r.get('code') == 200 and r['data'].get('unchanged') is True)
        step('跨文件GraphRAG返回原负责人', lambda: ask('GRAPHRAG'), answer_has('验收负责人甲'))
        step('真实模型更新第二版本', lambda: upload(1, 2, text_c), lambda r: r.get('code') == 200 and r['data'].get('version') == 2)
        step('图谱不再返回旧负责人关系', graph, lambda r: r.get('code') == 200 and '验收负责人乙' in json.dumps(r['data'], ensure_ascii=False) and '验收负责人甲' not in json.dumps(r['data'], ensure_ascii=False))
        for mode in ['GRAPHRAG', 'RAG', 'HYBRID']:
            step(mode + '使用新版本并附来源', lambda m=mode: ask(m), lambda r: answer_has('验收负责人乙')(r) and '验收负责人甲' not in r['data']['answer'] and all(s['version'] == 2 for s in r['data']['sources'] if s['documentId'] == ids[1]))
        step('删除设备资料并清理图谱', lambda: call('/knowledge/documents/' + ids[1] + '?version=3', method='DELETE'), lambda r: r.get('code') == 200 and r['data'].get('deleted') and not r['data'].get('cleanupPending'))
        step('删除后的旧版本不能重新发布', lambda: upload(1, 2, text_c), lambda r: r.get('code') == 400)
        step('指定已删除资料拒绝问答', lambda: ask('GRAPHRAG'), lambda r: r.get('code') == 403)
        step('剩余资料无法再回答已删除负责人', lambda: ask('GRAPHRAG', [ids[0]]), lambda r: r.get('code') == 200 and '验收负责人甲' not in r['data']['answer'] and '验收负责人乙' not in r['data']['answer'])
        success = True
    except Exception as error:
        failure = str(error)
        print(failure, file=sys.stderr, flush=True)
    finally:
        if token:
            try:
                for doc in call('/knowledge/documents').get('data', []):
                    if doc['documentId'] in ids:
                        response = call('/knowledge/documents/' + doc['documentId'] + '?version=' + str(doc['version'] + 1), method='DELETE')
                        cleanup.append({'documentId': doc['documentId'], 'response': response})
                for item in call('/knowledge/deletions').get('data', []):
                    if item['documentId'] in ids:
                        cleanup.append({'documentId': item['documentId'], 'response': call('/knowledge/documents/' + item['documentId'] + '?version=' + str(item['version']), method='DELETE')})
                remaining = [d for d in call('/knowledge/documents').get('data', []) if d['documentId'] in ids]
                pending = [d for d in call('/knowledge/deletions').get('data', []) if d['documentId'] in ids]
                if remaining or pending:
                    success = False
                    failure = (failure or '') + '; temporary fixture cleanup incomplete'
            except Exception as error:
                success = False
                failure = (failure or '') + '; cleanup: ' + str(error)
        output = Path(args.output)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps({'realModel': True, 'fixtureDocumentIds': ids, 'success': success, 'failure': failure, 'checks': results, 'cleanup': cleanup}, ensure_ascii=False, indent=2), encoding='utf-8')
    return 0 if success else 1


if __name__ == '__main__':
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    raise SystemExit(main())
