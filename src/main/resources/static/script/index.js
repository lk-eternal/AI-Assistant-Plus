function isMobileDevice() {
    return navigator.userAgent.match(/Mobile|iP(hone|od|ad)|Android|BlackBerry|IEMobile|Kindle|NetFront|Opera Mini|Windows CE|WebOS|SymbianOS/i);
}

const Api = {
    get(url, params) {
        return fetch(`${url}?${new URLSearchParams(params).toString()}`, {
            credentials: 'include',
            method: 'GET',
            headers: {'Content-Type': 'application/json;charset=UTF-8'},
        });
    },

    post(url, body) {
        return fetch(url, {
            credentials: 'include',
            method: 'POST',
            headers: {'Content-Type': 'application/json;charset=UTF-8'},
            body: JSON.stringify(body)
        });
    },

    put(url, body) {
        return fetch(url, {
            credentials: 'include',
            method: 'PUT',
            headers: {'Content-Type': 'application/json;charset=UTF-8'},
            body: JSON.stringify(body),
        });
    },

    delete(url, body) {
        return fetch(url, {
            credentials: 'include',
            method: 'DELETE',
            headers: {'Content-Type': 'application/json;charset=UTF-8'},
            body: JSON.stringify(body),
        });
    }
}

const UserService = {
    register(email, password) {
        return Api.post('/user/register', {email: email, password: password});
    },

    login(email, password, loadHistory) {
        return Api.post('/user/login', {email: email, password: password, loadHistory: loadHistory});
    },

    exit() {
        ChatService.stop();
        return Api.post('/user/exit');
    },

    validGpt4(code) {
        return Api.post(`/user/valid/gpt4/${code}`);
    },

    properties(properties) {
        return Api.post('/user/properties', properties);
    },
}

const ChatService = {
    plugins() {
        return Api.get('/api/plugins');
    },

    question(question) {
        return Api.post(`/api/question`, {question: question});
    },

    stop() {
        return Api.post(`/api/stop`);
    },

    clear() {
        return Api.delete(`/api/clear`);
    }
}

let loginRegister = {
    loginBox: null,
    loginFromBtn: null,
    emailInput: null,
    pwdInput: null,
    loadHistoryCheckBox: null,
    init() {
        this.loginBox = document.getElementById('loginBox');

        this.loginFromBtn = document.getElementById('loginFromBtn');
        this.loginFromBtn.addEventListener('click', () => this.open());

        this.emailInput = document.getElementById('email');
        this.pwdInput = document.getElementById('pwd');
        this.loadHistoryCheckBox = document.getElementById('loadHistory');

        document.getElementById('registerBtn').addEventListener('click', () => this.register());
        document.getElementById('loginBtn').addEventListener('click', () => this.login());

        let closeBtn = this.loginBox.querySelector('.closeBtn');
        closeBtn.addEventListener('click', () => this.close());

    },
    register() {
        let email = this.emailInput.value;
        let pwd = this.pwdInput.value;
        if (email !== undefined && email !== '' && pwd !== undefined && pwd !== '') {
            UserService.register(email, pwd)
                .then(r => {
                    if (r.ok) {
                        //TODO
                        alert("注册成功")
                        loginRegister.close();
                    } else {
                        window.alert("验证失败")
                    }
                })
                .catch(e => {
                    window.alert(e.message)
                });
        }
    },
    login() {
        let email = this.emailInput.value;
        let pwd = this.pwdInput.value;
        let loadHistory = this.loadHistoryCheckBox.checked;
        if (email !== undefined && email !== '' && pwd !== undefined && pwd !== '') {
            UserService.login(email, pwd, loadHistory)
                .then(r => {
                    if (r.ok) {
                        //TODO
                        alert("登录成功")
                        loginRegister.close();

                        //加载消息
                        chat.reset();
                        r.json().then(user => {
                            for (let message in user.messages) {
                                chat.add(message)
                            }
                        });

                        //加载设置

                    } else {
                        window.alert("验证失败")
                    }
                })
                .catch(e => {
                    window.alert(e.message)
                });
        }
    },
    open() {
        this.loginBox.style.display = 'flex';
    },
    close() {
        this.loginBox.style.display = 'none';
    }
}

let setting = {
    settingBox: null,
    settingBtn: null,
    init() {
        this.settingBox = document.getElementById('settingBox');

        this.settingBtn = document.getElementById('settingBtn');
        this.settingBtn.addEventListener('click', () => this.open());

        let closeBtn = this.settingBox.querySelector('.closeBtn');
        closeBtn.addEventListener('click', () => this.close());

        this.aiModel.init();
        this.pluginModel.init();
    },
    open() {
        this.settingBox.style.display = 'flex';
    },
    close() {
        this.settingBox.style.display = 'none';
        this.updateProperties();
    },
    updateProperties() {
        let configMap = {};
        let inputs = this.settingBox.querySelectorAll('input.config');
        for (let i = 0; i < inputs.length; i++) {
            let input = inputs[i];
            let key = input.name;
            let value;
            if (input.type === 'radio') {
                if (input.checked) {
                    value = input.value;
                }
            } else if (input.type === 'checkbox') {
                if (input.checked) {
                    value = input.value;
                    if (configMap[key] === undefined && value !== undefined) {
                        configMap[key] = [];
                    }
                }
            } else {
                value = input.value;
            }
            if (value !== undefined) {
                if (Array.isArray(configMap[key])) {
                    configMap[key].push(value);
                } else {
                    configMap[key] = value;
                }
            }
        }
        console.log(configMap)
        UserService.properties(configMap);
    },
    aiModel: {
        init() {
            let aiModels = document.querySelectorAll('input[name="aiModel"]');
            aiModels.forEach(am => {
                am.addEventListener('change', () => {
                    let aiModelValue = this.getValue();
                    if (aiModelValue !== 'gpt3.5' && aiModelValue !== 'gpt4') {
                        setting.pluginModel.disable('native');
                    } else {
                        setting.pluginModel.enable('native');
                    }
                    document.getElementById("gpt4CodeDiv").style.display = aiModelValue === 'gpt4' ? 'block' : 'none';
                });
            });
            let gpt4CodeDiv = document.getElementById("gpt4CodeDiv");
            let gpt4CodeValidBtn = document.getElementById("gpt4CodeValidBtn");
            gpt4CodeValidBtn.addEventListener("click", () => {
                let gpt4Code = document.getElementById("gpt4Code").value;
                if (gpt4Code !== undefined && gpt4Code !== '') {
                    UserService.validGpt4(gpt4Code)
                        .then(r => {
                            if (r.ok) {
                                gpt4CodeDiv.style.display = 'none';
                            } else {
                                window.alert("验证失败")
                            }
                        });
                }
            })
        },
        getValue() {
            return document.querySelector('input[name="aiModel"]:checked').value;
        },

        getAiModel(modelName) {
            return document.querySelector('input[name="aiModel"][value="' + modelName + '"]');
        },

        disable(modelName) {
            let model = this.getAiModel(modelName);
            if (model.checked) {
                this.getAiModel('gpt3.5').checked = true;
            }
            model.disabled = true;
        },

        enable(modelName) {
            this.getAiModel(modelName).disabled = false;
        },
    },
    pluginModel: {
        pluginListDiv: null,
        init() {
            ChatService.plugins().then(r => {
                if (r.ok) {
                    this.pluginListDiv = document.getElementById('pluginList');
                    r.json().then(pluginList => {
                        console.log(pluginList)

                        for (let i = 0; i < pluginList.length; i++) {
                            let plugin = pluginList[i];
                            let div = document.createElement('div');
                            let id = `plugin-${plugin.name}`;
                            div.innerHTML = `
                            <input id="${id}" class="config" type="checkbox" name="plugins" value="${plugin.name}">
                            <label for="plugin-${plugin.name}">${plugin.description}</label>
                            <div class="properties" style="display: none">
                            ${plugin.requireProperties.map(p => `
                                <p>${p.description}:<input class="config" name="plugin-${plugin.name}-${p.key}"></p>
                            `).join('\n')}
                            </div>
                        `;
                            this.pluginListDiv.appendChild(div);
                            let checkbox = document.getElementById(id);
                            checkbox.addEventListener("change", () => {
                                div.querySelector("div.properties").style.display = checkbox.checked ? 'block' : 'none';
                            })
                        }
                    });
                }
            })

            let pluginModels = document.querySelectorAll('input[name="pluginModel"]');
            pluginModels.forEach(tm => {
                tm.addEventListener('change', () => {
                    if (this.getValue() === 'native') {
                        setting.aiModel.disable('tyqw');
                        setting.aiModel.disable('gemini');
                    } else {
                        setting.aiModel.enable('tyqw');
                        setting.aiModel.enable('gemini');
                    }

                    if (this.getValue() !== 'none') {
                        this.pluginListDiv.style.display = 'block';
                    } else {
                        this.pluginListDiv.style.display = 'none';
                    }
                });
            });
        },
        getValue() {
            return document.querySelector('input[name="pluginModel"]:checked').value;
        },

        getModel(modelName) {
            return document.querySelector('input[name="pluginModel"][value="' + modelName + '"]');
        },

        disable(modelName) {
            let model = this.getModel(modelName);
            if (model.checked) {
                this.getModel('none').checked = true;
            }
            model.disabled = true;
        },

        enable(modelName) {
            this.getModel(modelName).disabled = false;
        },
    }
}

let chat = {
    chatBox: null,
    inputTextArea: null,
    sendBtn: null,
    stopBtn: null,
    stopReading: false,
    init() {
        this.chatBox = document.getElementById('chatBox');

        this.inputTextArea = document.getElementById('inputTextArea');
        this.inputTextArea.addEventListener('keydown', event => {
            if (event.key === 'Enter') {
                this.send();
            }
        });

        this.sendBtn = document.getElementById('sendBtn');
        this.sendBtn.addEventListener('click', () => this.send());

        this.stopBtn = document.getElementById('stopBtn');
        this.stopBtn.addEventListener('click', () => this.stop());

        let clearBtn = document.getElementById('clearBtn');
        clearBtn.addEventListener('click', () => this.clear());
    },
    add(content, div){
        if(div === undefined){
            div = document.createElement('div');
        }
        try {
            div.innerHTML = marked.parse(content);
        } catch (error) {
            console.error('marked parse error: ' + content);
            return;
        }

        div.querySelectorAll('pre code').forEach((el) => {
            hljs.highlightElement(el);
        });

        // 获取所有 p 元素
        let pElements = div.getElementsByTagName('p');
        for (let j = 0; j < pElements.length; j++) {
            pElements[j].innerHTML = pElements[j].textContent.replace(/\n/g, '<br>');
        }

        // 获取所有 pre 元素
        let preElements = div.getElementsByTagName('pre');

        // 为每个 pre 元素添加点击事件
        for (let j = 0; j < preElements.length; j++) {
            let copyButton = document.createElement('button');
            copyButton.textContent = '复制';
            copyButton.className = 'copy-button';
            preElements[j].appendChild(copyButton);

            copyButton.addEventListener('click', event => {
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
        this.chatBox.scrollTop = this.chatBox.scrollHeight;
    },
    send() {
        let question = this.inputTextArea.value;
        if (question === undefined || question === '') {
            question = this.inputTextArea.placeholder;
        }
        this.inputTextArea.value = '';

        let reqDiv = document.createElement('div');
        reqDiv.textContent = question;
        this.chatBox.insertBefore(reqDiv, this.stopBtn);

        this.inputTextArea.disabled = true;
        this.sendBtn.disabled = true;

        let respDiv = document.createElement('div');
        respDiv.innerHTML = '';
        respDiv.classList.add('loading');
        this.chatBox.insertBefore(respDiv, this.stopBtn);
        this.chatBox.scrollTop = this.chatBox.scrollHeight;

        ChatService.question(question)
            .then(async response => {
                if (!response.ok) {
                    console.log(response)
                    throw new Error(response.status + ':' + response.statusText);
                }

                this.stopBtn.style.display = "flex";

                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                const delimiter = '[PACKAGE_END]'

                let data = '';
                let textValue = '';
                while (!this.stopReading) {
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

                        chat.add(content, respDiv);
                    }
                }
            }).catch(err => {
            console.error('Error:', err);
            respDiv.classList.remove('loading');
            respDiv.innerHTML = `<div>${err.message}</div>`;
        }).finally(() => {
            this.stopReading = false;
            this.inputTextArea.disabled = false;
            this.sendBtn.disabled = false;
            if (!isMobileDevice()) {
                this.inputTextArea.focus();
            }
            this.stopBtn.style.display = "none";
            this.chatBox.scrollTop = this.chatBox.scrollHeight;
        });
    },
    stop() {
        this.stopReading = true;
        ChatService.stop();
    },
    clear() {
        ChatService.clear().then(() => {
            chat.reset();
            this.inputTextArea.disabled = false;
            this.sendBtn.disabled = false;
        });
    },
    reset(){
        let divs = this.chatBox.getElementsByTagName('div');
        for (let i = divs.length - 1; i > 0; i--) {
            this.chatBox.removeChild(divs[i]);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loginRegister.init();
    setting.init();
    chat.init();

    window.addEventListener("beforeunload", () => UserService.exit());
});
