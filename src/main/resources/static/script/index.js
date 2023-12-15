document.addEventListener('DOMContentLoaded', function() {

    let chatBox = document.getElementById('chatBox');

    let clearBtn = document.getElementById('clearBtn');
    let settingBtn = document.getElementById('settingBtn');
    let settingBox = document.getElementById('settingBox');
    let closeBtn = document.getElementById('closeBtn');

    let inputTextArea = document.getElementById('inputTextArea');
    let sendBtn = document.getElementById('sendBtn');

    window.addEventListener("beforeunload", clearMessages);

    clearBtn.addEventListener('click', clearMessages);
    settingBtn.addEventListener('click', openSettingModal);
    closeBtn.addEventListener('click', closeSettingModal);

    inputTextArea.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            sendMessage();
        }
    });
    sendBtn.addEventListener('click', sendMessage);

    let aiModels = document.querySelectorAll('input[name="ai-model"]');
    aiModels.forEach(aiModel => {
      aiModel.addEventListener('change', function() {
        let aiModel = document.querySelector('input[name="ai-model"]:checked').value;
        if(aiModel === 'tyqw'){
            let nativeModel = document.querySelector('input[name="tool-model"][value="native"]');
            let c = nativeModel.checked;
            if(c){
                document.querySelector('input[name="tool-model"][value="none"]').checked = true;
            }
            nativeModel.disabled = true;
        }else{
            let nativeModel = document.querySelector('input[name="tool-model"][value="native"]');
            nativeModel.disabled = false;
        }
      });
    });

    let toolModels = document.querySelectorAll('input[name="tool-model"]');
    toolModels.forEach(toolModel => {
      toolModel.addEventListener('change', function() {
        let toolModel = document.querySelector('input[name="tool-model"]:checked').value;
        if(toolModel === 'native'){
            let tyqwModel = document.querySelector('input[name="ai-model"][value="tyqw"]');
            let c = tyqwModel.checked;
            if(c){
                document.querySelector('input[name="ai-model"][value="gpt3.5"]').checked = true;
            }
            tyqwModel.disabled = true;
        }else{
            let tyqwModel = document.querySelector('input[name="ai-model"][value="tyqw"]');
            tyqwModel.disabled = false;
        }
      });
    });

    function openSettingModal() {
        settingBox.style.display = 'flex';
    }
    function closeSettingModal() {
        settingBox.style.display = 'none';
    }

    async function clearMessages() {
        let response = await fetch('/api', {
            method: 'DELETE'
        });

        chatBox.innerHTML = '<div>有何贵干?</div>';
        document.cookie = "cookieName=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        inputTextArea.disabled = false;
        sendBtn.disabled = false;
    }

    async function sendMessage() {
        let question = inputTextArea.value;
        if(question == undefined || question == ''){
            question = inputTextArea.placeholder;
        }
        inputTextArea.value = '';

        let reqDiv = document.createElement('div');
        reqDiv.textContent = question;
        chatBox.appendChild(reqDiv);

        inputTextArea.disabled = true;
        sendBtn.disabled = true;


        let respDiv = document.createElement('div');
        respDiv.innerHTML = '';
        respDiv.classList.add('loading');
        chatBox.appendChild(respDiv);
        chatBox.scrollTop = chatBox.scrollHeight;

        let toolModel = document.querySelector('input[name="tool-model"]:checked').value;
        let aiModel = document.querySelector('input[name="ai-model"]:checked').value;
        let gpt4Code = document.getElementById('gpt4Code').value;

        let response = await fetch('/api', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            },
            body: JSON.stringify({'aiModel': aiModel, 'toolModel': toolModel, 'question': question, 'gpt4Code': gpt4Code})
        });

        if (response.ok) {
            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            let data = '';
            while (true) {
                const { done, value } = await reader.read();
                respDiv.classList.remove('loading');
                if (done) {
                    break;
                }
                let packets = decoder.decode(value).split('\n');
                for (let i = 0; i < packets.length - 1; i++) {
                    const packet = packets[i];
                    let resp = JSON.parse(packet);
                    console.log(resp)

                    let content;
                    switch(resp.status){
                        case 'TYPING':
                            data += resp.message;
                            content = data;
                            break;
                        case 'FUNCTION_CALLING':
                            content = '正在调用' + resp.message + '工具获取信息...';
                            data = '';
                            break;
                        case 'ERROR':
                            content = '发生错误: ' + resp.message;
                            data = '';
                            break;
                    }

                    respDiv.innerHTML = marked.parse(content);

                    respDiv.querySelectorAll('pre code').forEach((el) => {
                        hljs.highlightElement(el);
                    });

                    // 获取 p 元素中的所有 pre 元素
                    let preElements = respDiv.getElementsByTagName('pre');

                    // 为每个 pre 元素添加点击事件
                    for (let j = 0; j < preElements.length; j++) {
                        let copyButton = document.createElement('button');
                        copyButton.textContent = '复制';
                        copyButton.className = 'copy-button';
                        preElements[j].appendChild(copyButton);

                        copyButton.addEventListener('click', function(event) {
                            let textarea = document.createElement('textarea');
                            textarea.value = event.target.parentNode.textContent.replace('复制', '');
                            document.body.appendChild(textarea);
                            textarea.select();
                            document.execCommand('copy');
                            document.body.removeChild(textarea);

                            let alert = document.createElement('div');
                            alert.textContent = '复制成功！';
                            alert.style.position = 'fixed';
                            alert.style.bottom = '50%';
                            alert.style.left = '50%';
                            alert.style.transform = 'translate(-50%, 50%)';
                            alert.style.backgroundColor = '#20B2AA';
                            alert.style.color = 'white';
                            alert.style.padding = '10px';
                            alert.style.borderRadius = '5px';
                            document.body.appendChild(alert);
                            setTimeout(function() {
                                document.body.removeChild(alert);
                            }, 2000);
                        });
                    }
                    chatBox.scrollTop = chatBox.scrollHeight;
                }
            }
        }else{
            respDiv.classList.remove('loading');
            console.error('Error:', response.status, response.statusText);
            respDiv.innerHTML = '<div>' + response.status + ':' + response.statusText + '</div>';
            chatBox.scrollTop = chatBox.scrollHeight;
        }
        inputTextArea.disabled = false;
        sendBtn.disabled = false;
    }
});