"""Actual browser acceptance against the locally running unified workspace.
Uses the existing demo account and a temporary, clearly labelled PDF. Cloud calls are real.
"""
import argparse, hashlib, json, os, pathlib, uuid
from playwright.sync_api import sync_playwright

def pdf(text):
    stream=f'BT /F1 12 Tf 35 750 Td ({text}) Tj ET'.encode('ascii')
    objects=[b'<< /Type /Catalog /Pages 2 0 R >>',b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',b'<< /Length '+str(len(stream)).encode()+b' >>\nstream\n'+stream+b'\nendstream']
    data=b'%PDF-1.4\n';offsets=[0]
    for index,obj in enumerate(objects,1):offsets.append(len(data));data+=str(index).encode()+b' 0 obj\n'+obj+b'\nendobj\n'
    start=len(data);data+=b'xref\n0 6\n0000000000 65535 f \n'
    for offset in offsets[1:]:data+=f'{offset:010d} 00000 n \n'.encode()
    return data+f'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n{start}\n%%EOF'.encode()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);parser.add_argument('--work',required=True);args=parser.parse_args()
    output=pathlib.Path(args.output);output.mkdir(exist_ok=True,parents=True);work=pathlib.Path(args.work);work.mkdir(exist_ok=True,parents=True)
    original=pdf('UI23Process uses UI23Motor. UI23Motor is maintained by Dana.')
    updated=pdf('UI23Process uses UI23Motor. UI23Motor is maintained by Leo.')
    first=work/'ui23-v1.pdf';second=work/'ui23-v2.pdf';first.write_bytes(original);second.write_bytes(updated)
    errors=[];receipt={}
    with sync_playwright() as p:
        browser=p.chromium.launch(executable_path=r'C:\Program Files\Google\Chrome\Application\chrome.exe',headless=True)
        context=browser.new_context(viewport={'width':1500,'height':1100});page=context.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
        page.goto('http://127.0.0.1:5176/login');page.locator('#login-user-code').fill(os.getenv('MES_TEST_USER_CODE','109001'));page.locator('#login-password').fill(os.getenv('MES_TEST_PASSWORD','123456'));page.locator('button[type=submit]').click();page.wait_for_url('**/dashboard',timeout=30000)
        page.goto('http://127.0.0.1:5176/knowledge');page.get_by_role('heading',name='制造知识，连接成图').wait_for()
        page.get_by_role('button',name='检测连接').click();page.get_by_text('Neo4j：已连接',exact=True).wait_for(timeout=30000)
        old=page.locator('.document-card').filter(has_text='A100工艺演示（虚构）')
        if old.get_by_role('button',name='同步/重试').count():
            with page.expect_response(lambda r:'/retry?' in r.url,timeout=180000) as sync:old.get_by_role('button',name='同步/重试').click()
            assert sync.value.json()['data']['status']=='READY'
        title='联合UI-PDF验收（临时）'
        page.get_by_role('button',name='导入资料',exact=True).click();dialog=page.get_by_role('dialog');dialog.get_by_label('资料标题').fill(title);dialog.locator('input[type=file]').set_input_files(str(first))
        with page.expect_response(lambda r:r.url.endswith('/api/rag/documents') and r.request.method=='POST',timeout=180000) as upload:dialog.get_by_role('button',name='保存并构建双索引').click()
        result=upload.value.json();assert result['data']['status']=='READY',result;receipt['upload']=result['data'];id=result['data']['documentId']
        card=page.locator('.document-card').filter(has_text=title);card.wait_for()
        with page.expect_download() as downloading:card.get_by_role('button',name='下载',exact=True).click()
        downloaded=pathlib.Path(downloading.value.path()).read_bytes();assert hashlib.sha256(downloaded).digest()==hashlib.sha256(original).digest();receipt['downloadExact']=True
        card.get_by_role('button',name='更新',exact=True).click();dialog=page.get_by_role('dialog');dialog.locator('input[type=file]').set_input_files(str(second))
        with page.expect_response(lambda r:r.url.endswith('/api/rag/documents') and r.request.method=='POST',timeout=180000) as update:dialog.get_by_role('button',name='保存并构建双索引').click()
        result=update.value.json();assert result['data']['status']=='READY' and result['data']['version']==2,result;receipt['update']=result['data']
        page.locator('.scope-toolbar select').select_option(id);page.wait_for_timeout(800)
        page.locator('.mode-row select').select_option('RAG');page.get_by_role('textbox',name='知识库问题').fill('Who maintains UI23Motor?')
        with page.expect_response(lambda r:r.url.endswith('/api/knowledge/messages'),timeout=180000) as answering:page.get_by_role('button',name='提问',exact=True).click()
        answer=answering.value.json()['data'];assert 'Leo' in answer['answer'] and answer['sources'][0]['version']==2 and '第1页' in answer['sources'][0]['location'],answer;receipt['pdfAnswer']=answer
        card.get_by_role('button',name='删除',exact=True).click()
        with page.expect_response(lambda r:'/api/knowledge/documents/' in r.url and r.request.method=='DELETE',timeout=60000) as deleting:page.get_by_role('dialog').get_by_role('button',name='确认删除',exact=True).click()
        result=deleting.value.json()['data'];assert result['deleted'] and not result['cleanupPending'],result;receipt['delete']=result;card.wait_for(state='detached')
        page.locator('.mode-row select').select_option('HYBRID');page.get_by_role('textbox',name='知识库问题').fill('T23点胶工序使用的设备由谁负责维护？')
        with page.expect_response(lambda r:r.url.endswith('/api/knowledge/messages'),timeout=180000) as answering:page.get_by_role('button',name='提问',exact=True).click()
        answer=answering.value.json()['data'];assert '王工' in answer['answer'] and len(answer['sources'])>=2,answer;receipt['hybridAnswer']=answer
        page.locator('.source-card summary').first.click();page.screenshot(path=str(output/'任务二三-融合问答.png'),full_page=True)
        page.get_by_role('button',name='知识图谱',exact=True).click();page.get_by_role('button',name='查看T23点胶机的关系',exact=True).click();page.screenshot(path=str(output/'任务二三-知识图谱.png'),full_page=True)
        assert not errors,errors;receipt['pageErrors']=errors
        (work/'unified-ui-acceptance.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
        print(json.dumps({'ui':'PASS','pdfUploadDownloadUpdateDelete':True,'pageLocation':True,'hybridSources':len(answer['sources']),'pageErrors':errors},ensure_ascii=False));context.close();browser.close()

if __name__=='__main__':main()
