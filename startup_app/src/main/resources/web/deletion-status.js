'use strict';
document.getElementById('deletion-status-form').addEventListener('submit',async event=>{
 event.preventDefault();const send=document.getElementById('deletion-status-submit'),input=document.getElementById('deletion-status-key'),result=document.getElementById('deletion-status-result');send.disabled=true;result.textContent='Checking…';
 try{const boot=await fetch('/api/bootstrap',{cache:'no-store'});if(!boot.ok)throw Error('Unable to open a secure session.');const session=await boot.json();const response=await fetch('/api/account-deletion/status',{method:'POST',cache:'no-store',headers:{'Content-Type':'application/json','X-CSRF-Token':session.csrf_token},body:JSON.stringify({receipt_token:input.value})});const data=await response.json();if(!response.ok)throw Error(response.status===404?'No committed result was found for this key. This does not prove the request failed; an interrupted transaction may still be settling.':response.status===429?'Too many status checks. Try again later.':'Unable to verify this result.');result.textContent='Active application data removed at '+data.completed_at+'. External copies and backups: review required. This receipt does not certify full production erasure.';}
 catch(error){result.textContent=error.message||'Status is unavailable. Keep your key and check again.';}
 finally{input.value='';send.disabled=false;}
});
