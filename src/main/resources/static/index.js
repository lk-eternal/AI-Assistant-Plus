document.addEventListener('DOMContentLoaded', function() {
    var inputTextArea = document.getElementById('inputTextArea');
    var chatBox = document.getElementById('chatBox');
    var sendBtn = document.getElementById('sendBtn');
    var clearBtn = document.getElementById('clearBtn');

    window.addEventListener("beforeunload", clearMessages);
    clearBtn.addEventListener('click', clearMessages);
    sendBtn.addEventListener('click', sendMessage);

    inputTextArea.addEventListener('keydown', function(event) {
        if (event.key === 'Enter') {
            sendMessage();
        }
    });

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
        var message = inputTextArea.value;
        if(message == undefined || message == ''){
            message = inputTextArea.placeholder;
        }
        inputTextArea.value = '';

        var divElement = document.createElement('div');
        divElement.textContent = message;
        chatBox.appendChild(divElement);
        chatBox.scrollTop = chatBox.scrollHeight;

        inputTextArea.disabled = true;
        sendBtn.disabled = true;

        var response = await fetch('/api', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            },
            body: message
        });

        if (response.ok) {
            var data = await response.text();
            var divElement = document.createElement('div');
            divElement.innerHTML = marked.parse(data);

            // 获取 p 元素中的所有 pre 元素
            var preElements = divElement.getElementsByTagName('pre');

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

            chatBox.appendChild(divElement);
        } else {
            console.error('Error:', response.status, response.statusText);
            chatBox.innerHTML += '<div>' + response.status + ':' + response.statusText + '</div>';
        }
        chatBox.scrollTop = chatBox.scrollHeight;
        inputTextArea.disabled = false;
        sendBtn.disabled = false;
    }
});