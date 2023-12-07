document.addEventListener('DOMContentLoaded', function() {

    var chatBox = document.getElementById('chatBox');

    var clearBtn = document.getElementById('clearBtn');
    var settingBtn = document.getElementById('settingBtn');
    var settingBox = document.getElementById('settingBox');
    var closeBtn = document.getElementById('closeBtn');

    var inputTextArea = document.getElementById('inputTextArea');
    var sendBtn = document.getElementById('sendBtn');

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

    var aiModels = document.querySelectorAll('input[name="ai-model"]');
    aiModels.forEach(aiModel => {
      aiModel.addEventListener('change', function() {
        var aiModel = document.querySelector('input[name="ai-model"]:checked').value;
        if(aiModel === 'tyqw'){
            var nativeModel = document.querySelector('input[name="tool-model"][value="native"]');
            var c = nativeModel.checked;
            if(c){
                document.querySelector('input[name="tool-model"][value="none"]').checked = true;
            }
            nativeModel.disabled = true;
        }else{
            var nativeModel = document.querySelector('input[name="tool-model"][value="native"]');
            nativeModel.disabled = false;
        }
      });
    });

    var toolModels = document.querySelectorAll('input[name="tool-model"]');
    toolModels.forEach(toolModel => {
      toolModel.addEventListener('change', function() {
        var toolModel = document.querySelector('input[name="tool-model"]:checked').value;
        if(toolModel === 'native'){
            var tyqwModel = document.querySelector('input[name="ai-model"][value="tyqw"]');
            var c = tyqwModel.checked;
            if(c){
                document.querySelector('input[name="ai-model"][value="gpt3.5"]').checked = true;
            }
            tyqwModel.disabled = true;
        }else{
            var tyqwModel = document.querySelector('input[name="ai-model"][value="tyqw"]');
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
        var response = await fetch('/api', {
            method: 'DELETE'
        });

        chatBox.innerHTML = '<div>有何贵干?</div>';
        document.cookie = "cookieName=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        inputTextArea.disabled = false;
        sendBtn.disabled = false;
    }

    async function sendMessage() {
        var question = inputTextArea.value;
        if(question == undefined || question == ''){
            question = inputTextArea.placeholder;
        }
        inputTextArea.value = '';

        var reqDiv = document.createElement('div');
        reqDiv.textContent = question;
        chatBox.appendChild(reqDiv);

        inputTextArea.disabled = true;
        sendBtn.disabled = true;


        var respDiv = document.createElement('div');
        respDiv.innerHTML = '';
        respDiv.classList.add('loading');
        chatBox.appendChild(respDiv);
        chatBox.scrollTop = chatBox.scrollHeight;

        var toolModel = document.querySelector('input[name="tool-model"]:checked').value;
        var aiModel = document.querySelector('input[name="ai-model"]:checked').value;
        var gpt4Code = document.getElementById('gpt4Code').value;

        var response = await fetch('/api', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            },
            body: JSON.stringify({'aiModel': aiModel, 'toolModel': toolModel, 'question': question, 'gpt4Code': gpt4Code})
        });

        if (response.ok) {
            var data = await response.text();
            respDiv.innerHTML = marked.parse(data);

            respDiv.querySelectorAll('pre code').forEach((el) => {
                hljs.highlightElement(el);
            });

            // 获取 p 元素中的所有 pre 元素
            var preElements = respDiv.getElementsByTagName('pre');

            // 为每个 pre 元素添加点击事件
            for (var i = 0; i < preElements.length; i++) {
                var copyButton = document.createElement('button');
                copyButton.textContent = '复制';
                copyButton.className = 'copy-button';
                preElements[i].appendChild(copyButton);

                copyButton.addEventListener('click', function(event) {
                    var textarea = document.createElement('textarea');
                    textarea.value = event.target.parentNode.textContent.replace('复制', '');
                    document.body.appendChild(textarea);
                    textarea.select();
                    document.execCommand('copy');
                    document.body.removeChild(textarea);

                    var alert = document.createElement('div');
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
        } else {
            console.error('Error:', response.status, response.statusText);
            respDiv.innerHTML = '<div>' + response.status + ':' + response.statusText + '</div>';
        }
        respDiv.classList.remove('loading');
        chatBox.scrollTop = chatBox.scrollHeight;
        inputTextArea.disabled = false;
        sendBtn.disabled = false;
    }
});