"""Opt-in real login + real Ark + real Neo4j acceptance. Leaves clearly labelled demo files.
Run: python verify_unified_knowledge_live.py --live --report <path>
No provider key is read, printed or stored by this client; the backend owns provider access.
"""
import argparse, hashlib, io, json, os, pathlib, urllib.request, urllib.error, uuid, zipfile

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--live',action='store_true');parser.add_argument('--base',default='http://127.0.0.1:8086');parser.add_argument('--report',required=True)
    args=parser.parse_args()
    if not args.live: raise SystemExit('Pass --live to permit a small number of paid model calls.')
    token=''
    def request(path,body=None,method=None,content_type='application/json',raw=False):
        data=body if isinstance(body,bytes) else (json.dumps(body,ensure_ascii=False).encode('utf-8') if body is not None else None)
        headers={'Content-Type':content_type}
        if token:headers['Authorization']='Bearer '+token
        req=urllib.request.Request(args.base+path,data=data,headers=headers,method=method)
        with urllib.request.urlopen(req,timeout=600) as r:
            response=r.read()
        if raw:return response
        value=json.loads(response);assert value['code']==200,value
        return value['data']
    login=request('/api/auth/login',{'userCode':os.getenv('MES_TEST_USER_CODE','109001'),'password':os.getenv('MES_TEST_PASSWORD','123456')})
    token=login['token']
    status=request('/api/knowledge/status?probe=true')
    assert status['neo4j']=='已连接' and status['vectorRetrieverConnected'],status
    assert '向量' in status['ragProvider'],status
    def multipart(id,version,title,filename,content):
        boundary='mes-'+uuid.uuid4().hex;parts=[]
        for key,value in [('documentId',id),('version',str(version)),('title',title)]:
            parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: application/octet-stream\r\n\r\n'.encode()+content+b'\r\n')
        parts.append(f'--{boundary}--\r\n'.encode())
        return request('/api/rag/documents',b''.join(parts),'POST','multipart/form-data; boundary='+boundary)
    ids=['unified-demo-process','unified-demo-maintenance'];existing={d['documentId']:d for d in request('/api/knowledge/documents')}
    process='本资料为课程联合验收的虚构资料。T23鼠标需要T23点胶工序。T23点胶工序使用T23点胶机。'
    owner='本资料为课程联合验收的虚构资料。T23点胶机由王工负责日常维护。T23点胶机每周检查喷嘴。'
    out=io.BytesIO()
    with zipfile.ZipFile(out,'w') as z:
        z.writestr('[Content_Types].xml','<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/></Types>')
        z.writestr('word/document.xml','<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>'+owner+'</w:t></w:r></w:p></w:body></w:document>')
    uploads=[]
    for id,title,filename,content in [(ids[0],'联合验收-T23工艺（虚构）','T23工艺.md',process.encode()),(ids[1],'联合验收-T23点检（虚构）','T23点检.docx',out.getvalue())]:
        if id not in existing:
            value=multipart(id,1,title,filename,content);assert value['status']=='READY',value;uploads.append(value)
        downloaded=request('/api/rag/documents/'+id+'/download',raw=True)
        if id not in existing or not filename.endswith('.docx'):
            assert hashlib.sha256(downloaded).digest()==hashlib.sha256(content).digest()
        else:
            # ZIP timestamps vary when the fixture is regenerated; compare the stored DOCX body
            # on subsequent runs. A new upload is always checked byte-for-byte above.
            with zipfile.ZipFile(io.BytesIO(downloaded)) as z,zipfile.ZipFile(io.BytesIO(content)) as expected:
                assert z.read('word/document.xml')==expected.read('word/document.xml')
    graph=request('/api/knowledge/graph?documentIds='+','.join(ids))
    # Spring list parameters accept a comma separated documentIds value.
    assert any(f['target']=='T23点胶机' for f in graph['facts']),graph
    answers={}
    for mode in ['RAG','GRAPHRAG','HYBRID']:
        reply=request('/api/knowledge/messages',{'question':'T23点胶工序使用的设备由谁负责维护？','mode':mode,'documentIds':ids})
        assert reply['sources'] and '王工' in reply['answer'],reply
        if mode!='RAG':
            assert len(set(s['documentId'] for s in reply['sources']))==2,reply
            assert any(f['target']=='王工' for f in reply['facts']),reply
        for source in reply['sources']:assert source['chunkId'] in [f['chunkId'] for f in graph['facts']]
        answers[mode]=reply
    for path in ['/api/knowledge/documents','/api/rag/documents']:
        try:urllib.request.urlopen(args.base+path);raise AssertionError('Anonymous access accepted')
        except urllib.error.HTTPError as e:assert e.code==401,e.code
    try:request('/api/rag/messages',{'question':'资料','mode':'RAG','userId':999});raise AssertionError('Forged owner accepted')
    except urllib.error.HTTPError as e:assert e.code==400,e.code
    report={'status':status,'uploads':uploads,'graph':graph,'answers':answers,'anonymous':'401','forgedOwner':'400','provider':'actual user-configured Ark','graphDatabase':'actual local Neo4j'}
    pathlib.Path(args.report).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({'result':'PASS','modes':list(answers),'relations':len(graph['facts']),'sources':{k:len(v['sources']) for k,v in answers.items()}},ensure_ascii=False))

if __name__=='__main__':main()
