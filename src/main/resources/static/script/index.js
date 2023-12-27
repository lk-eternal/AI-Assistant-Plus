document.addEventListener('DOMContentLoaded', function () {

    let chatBox = document.getElementById('chatBox');

    let clearBtn = document.getElementById('clearBtn');
    let settingBtn = document.getElementById('settingBtn');
    let settingBox = document.getElementById('settingBox');
    let closeBtn = document.getElementById('closeBtn');

    let inputTextArea = document.getElementById('inputTextArea');
    let sendBtn = document.getElementById('sendBtn');
    let stopBtn = document.getElementById('stopBtn');

    window.addEventListener("beforeunload", clearMessages);

    clearBtn.addEventListener('click', clearMessages);
    settingBtn.addEventListener('click', openSettingModal);
    closeBtn.addEventListener('click', closeSettingModal);

    inputTextArea.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') {
            sendMessage();
        }
    });
    sendBtn.addEventListener('click', sendMessage);
    stopBtn.addEventListener('click', stopMessage);

    let aiModel = {
        getAiModelValue() {
            return document.querySelector('input[name="ai-model"]:checked').value;
        },

        getAiModel(modelName) {
            return document.querySelector('input[name="ai-model"][value="' + modelName + '"]');
        },

        disableAiModel(modelName) {
            let model = this.getAiModel(modelName);
            if (model.checked) {
                this.getAiModel('gpt3.5').checked = true;
            }
            model.disabled = true;
        },

        enableAiModel(modelName) {
            this.getAiModel(modelName).disabled = false;
        },
    }

    let toolModel = {
        getToolModelValue() {
            return document.querySelector('input[name="tool-model"]:checked').value;
        },

        getToolModel(modelName) {
            return document.querySelector('input[name="tool-model"][value="' + modelName + '"]');
        },

        disableToolModel(modelName) {
            let model = this.getToolModel(modelName);
            if (model.checked) {
                this.getToolModel('none').checked = true;
            }
            model.disabled = true;
        },

        enableToolModel(modelName) {
            this.getToolModel(modelName).disabled = false;
        },
    }

    let toolModels = document.querySelectorAll('input[name="tool-model"]');
    toolModels.forEach(tm => {
        tm.addEventListener('change', function () {
            if (toolModel.getToolModelValue() === 'native') {
                aiModel.disableAiModel('tyqw');
                aiModel.disableAiModel('gemini');
            } else {
                aiModel.enableAiModel('tyqw');
                aiModel.enableAiModel('gemini');
            }
        });
    });

    let aiModels = document.querySelectorAll('input[name="ai-model"]');
    aiModels.forEach(am => {
        am.addEventListener('change', function () {
            let aiModelValue = aiModel.getAiModelValue();
            if (aiModelValue !== 'gpt3.5' && aiModelValue !== 'gpt4') {
                toolModel.disableToolModel('native');
            } else {
                toolModel.enableToolModel('native');
            }
        });
    });

    function openSettingModal() {
        settingBox.style.display = 'flex';
    }

    function closeSettingModal() {
        settingBox.style.display = 'none';
    }

    function isMobileDevice() {
        return navigator.userAgent.match(/Mobile|iP(hone|od|ad)|Android|BlackBerry|IEMobile|Kindle|NetFront|Opera Mini|Windows CE|WebOS|SymbianOS/i);
    }

    function clearMessages() {
        fetch('/api', {
            method: 'DELETE'
        }).then(() => {
            let divs = chatBox.getElementsByTagName('div');
            for (let i = divs.length - 1; i > 0; i--) {
                chatBox.removeChild(divs[i]);
            }

            document.cookie = "cookieName=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
            inputTextArea.disabled = false;
            sendBtn.disabled = false;
        });
    }

    let stopReading = false;

    function sendMessage() {
        let question = inputTextArea.value;
        if (question === undefined || question === '') {
            question = inputTextArea.placeholder;
        }
        inputTextArea.value = '';

        let reqDiv = document.createElement('div');
        reqDiv.textContent = question;
        chatBox.insertBefore(reqDiv, stopBtn);

        inputTextArea.disabled = true;
        sendBtn.disabled = true;


        let respDiv = document.createElement('div');
        respDiv.innerHTML = '';
        respDiv.classList.add('loading');
        chatBox.insertBefore(respDiv, stopBtn);
        chatBox.scrollTop = chatBox.scrollHeight;

        let gpt4Code = document.getElementById('gpt4Code').value;

        fetch('/api', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            },
            body: JSON.stringify({
                'action': 'question',
                'aiModel': aiModel.getAiModelValue(),
                'toolModel': toolModel.getToolModelValue(),
                'question': question,
                'gpt4Code': gpt4Code
            })
        }).then(async response => {
            if (!response.ok) {
                throw new Error(response.status + ':' + response.statusText);
            }

            stopBtn.style.display = "flex";

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            const delimiter = '[PACKAGE_END]'

            let data = '';
            let textValue = '';
            while (!stopReading) {
                const {done, value} = await reader.read();
                respDiv.classList.remove('loading');
                if (done) {
                    break;
                }
                textValue += decoder.decode(value);
                if (!textValue.includes(delimiter)) {
                    continue;
                }
                let packets = textValue.split(delimiter);
                let packetSize;
                if (textValue.endsWith(delimiter)) {
                    packetSize = packets.length;
                    textValue = '';
                } else {
                    packetSize = packets.length - 1;
                    textValue = packets[packets.length - 1];
                }

                for (let i = 0; i < packetSize - 1; i++) {
                    const packet = packets[i];
                    let resp;
                    try {
                        resp = JSON.parse(packet);
                    } catch (error) {
                        console.error('JSON parse error: ' + packet);
                        resp = {status: 'ERROR', message: error};
                    }

                    let content;
                    switch (resp.status) {
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

                    try {
                        respDiv.innerHTML = marked.parse(content);
                    } catch (error) {
                        console.error('marked parse error: ' + content);
                        continue;
                    }

                    respDiv.querySelectorAll('pre code').forEach((el) => {
                        hljs.highlightElement(el);
                    });

                    // 获取所有 p 元素
                    let pElements = respDiv.getElementsByTagName('p');
                    for (let j = 0; j < pElements.length; j++) {
                        pElements[j].innerHTML = pElements[j].textContent.replace(/\n/g, '<br>');
                    }

                    // 获取所有 pre 元素
                    let preElements = respDiv.getElementsByTagName('pre');

                    // 为每个 pre 元素添加点击事件
                    for (let j = 0; j < preElements.length; j++) {
                        let copyButton = document.createElement('button');
                        copyButton.textContent = '复制';
                        copyButton.className = 'copy-button';
                        preElements[j].appendChild(copyButton);

                        copyButton.addEventListener('click', function (event) {
                            try {
                                navigator.clipboard.writeText(event.target.parentNode.textContent.replace('复制', ''));
                            } catch (err) {
                                console.error('Failed to copy text:', err);
                            }

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
                            setTimeout(function () {
                                document.body.removeChild(alert);
                            }, 2000);
                        });
                    }
                    chatBox.scrollTop = chatBox.scrollHeight;
                }
            }
        }).catch(err => {
            console.error('Error:', err);
            respDiv.classList.remove('loading');
            respDiv.innerHTML = `<div>${err.message}</div>`;
        }).finally(() => {
            stopReading = false;
            inputTextArea.disabled = false;
            sendBtn.disabled = false;
            if (!isMobileDevice()) {
                inputTextArea.focus();
            }
            stopBtn.style.display = "none";
            chatBox.scrollTop = chatBox.scrollHeight;
        });
    }

    function stopMessage() {
        stopReading = true;
        fetch('/api', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            },
            body: JSON.stringify({'action': 'stop'})
        });
    }
});