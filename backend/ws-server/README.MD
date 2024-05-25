## 📝 WebSocket endpoints documentation

<details>
<summary>👥 Group chats</summary>

### Join in the chat

Message structure:

```
path=/chat/group/{chatId}/join
```

Response message example:

```json
{
  "action": "JOIN",
  "timestamp": 1710622097.792737500,
  "chatId": "65f605253b9b7c0d6dab5546",
  "senderId": 10,
  "memberId": null,
  "memberDetails": {
    "username": "@danielmartinez",
    "name": "Daniel Martinez",
    "avatarPath": null,
    "online": true
  }
}
```

<br/>

### Leave from the chat

Message structure:

```
path=/chat/group/{chatId}/leave
```

Response message is absent if you were a member of the chat.
In other case you will receive message with `ERROR` action:

```json
{
  "action": "LEAVE",
  "timestamp": 1713443892.540455500,
  "senderId": 34,
  "chatId": "6621128c182be4335fc0a6e5"
}
```

<br/>

<details>
<summary>Edit chat members</summary>

### ➕ Add member to the chat

Message structure:
```
path=/chat/group/{chatId}/add_member/{userId}
```

Response message example:
```json
{
  "action": "ADD",
  "timestamp": 1713443949.902010300,
  "senderId": 1,
  "chatId": "6621128c182be4335fc0a6e5",
  "memberId": 10,
  "memberDetails": {
    "username": "@danielmartinez",
    "name": "Daniel Martinez",
    "avatarPath": null,
    "online": false
  }
}
```

Possible errors:

- Do not have permissions

```json
{
  "action": "ERROR",
  "timestamp": 1713443892.540455500,
  "error": "Permissions denied",
  "path": "/chat/group/6621128c182be4335fc0a6e5/add_member/10"
}
```

- Not a member of chat

```json
{
    "action": "ERROR",
    "timestamp": 1713443892.540455500,
    "error": "You are not member of chat",
    "path": "/chat/group/6621128c182be4335fc0a6e5/add_member/10"
}
```

<br/>

### ➖ Kick from the chat

Message structure:

```
path=/chat/group/{chatId}/kick/{memberId}
```

Response message:
```json
{
  "action": "KICK",
  "timestamp": 1713460587.891633600,
  "senderId": 1,
  "chatId": "6621128c182be4335fc0a6e5",
  "memberId": 10,
  "memberDetails": {
    "username": "@danielmartinez",
    "name": "Daniel Martinez",
    "avatarPath": null,
    "online": true
  }
}
```

Possible errors:

- Do not have permissions

```json
{
  "action": "ERROR",
  "timestamp": 1713443892.540455500,
  "error": "Permissions denied",
  "path": "/chat/group/6621128c182be4335fc0a6e5/kick/10"
}
```

- Member not found

```json
{
  "action": "ERROR",
  "timestamp": 1713443892.540455500,
  "error": "Member not found",
  "path": "/chat/group/6621128c182be4335fc0a6e5/kick/23"
}
```

<br/>

### ✏️ Change member role in the chat

Message structure:

```
path=/chat/group/{chatId}/set_role/{memberId}

{
  "role": string
}
```

Response message example:

```json
{
  "action": "SET_ROLE",
  "timestamp": 1713534527.594477900,
  "senderId": 1,
  "chatId": "6621128c182be4335fc0a6e5",
  "memberId": 10,
  "role": "ADMIN"
}
```

Possible errors:

- Do not have permissions

```json
{
  "action": "ERROR",
  "timestamp": 1713443892.540455500,
  "error": "Permissions denied",
  "path": "/chat/group/6621128c182be4335fc0a6e5/set_role/10"
}
```

- Member not found

```json
{
  "action": "ERROR",
  "timestamp": 1713443892.540455500,
  "error": "Member not found",
  "path": "/chat/group/6621128c182be4335fc0a6e5/set_role/11320"
}
```

</details>

</details>

<br/>

<details>
<summary>💬 Messaging</summary>

### 📨 Send a text message

Message structure is below. It is recommended to use UUID for temporary message id on the client.

```
path=/chat/{chatId}/send

{
  "tmpMessageId": string,
  "text": string
}
```

At first, you receive the bind message:

```json
{
  "action": "BIND",
  "chatId": "65f605253b9b7c0d6dab5546",
  "tmpMessageId": "3ba88cab-8ca5-48e7-aff4-ba689fccee70",
  "messageId": "65f607523b9b7c0d6dab5547"
}
```

And then every member of the destination chat will receive your message:

```json
{
  "action": "MESSAGE",
  "timestamp": 1710622546.515123400,
  "id": "65f607523b9b7c0d6dab5547",
  "chatId": "65f605253b9b7c0d6dab5546",
  "senderId": 34,
  "text": "Hi everyone!"
}
```

But if you try to send message to the chat you are not member in,
you will get message with `ERROR` action:

```json
{
  "action": "ERROR",
  "timestamp": 1710622677.980591300,
  "error": "You are not member of chat",
  "path": "/chat/65fca83ea94fa31e8a726f99/send"
}
```

<br/>

### ✏️ Edit text message

Message structure:

```
path=/chat/edit_text_message/{messageId}

[text]
```

Response message example:
```json
{
  "action": "UPD_MESSAGE",
  "timestamp": 1710622677.980591300,
  "senderId": 1,
  "chatId": "66054151e162397346b28554",
  "id": "66068a1a0f2b5c2763c23a67",
  "text": "update message from websockets"
}
```

Possible errors:

- Message not found

```json
{
  "action": "ERROR",
  "timestamp": 1710622677.980591300,
  "error": "Requested message not found",
  "path": "/chat/edit_text_message/66068a1a0f2b5c2763c23a7"
}
```

- Not a member of chat or not a message sender

```json
{
  "action": "ERROR",
  "timestamp": 1710622677.980591300,
  "error": "Do not have permissions to modify the message",
  "path": "/chat/edit_text_message/66068a1a0f2b5c2763c23a67"
}
```

<br/>

### 📎 Send a file message

Message structure:

```
path=/chat/{chatId}/send_file

{
  "id": integer,
  "createdAt": number,
  "filename": string,
  "contentType": string,
  "size": integer
}
```

Response message example:

```json
{
  "action": "FILE",
  "timestamp": 1714228196.375171300,
  "senderId": 1,
  "chatId": "662ce8cc905e8b2151fca5e2",
  "id": "662d0be46dc0dc6dc0ffa9c5",
  "filename": "masm32v11r.zip",
  "contentType": "application/zip",
  "size": 5012275
}
```

Possible errors:

- Invalid payload message data. Even if you set wrong file size, message still can not be found

```json
{
  "action": "ERROR",
  "timestamp": 1714227975.459646500,
  "error": "Uploaded file message not found",
  "path": "/chat/662ce8cc905e8b2151fca5e2/send_file"
}
```

<br/>

### ➖ Remove message

Message structure:

```
path=/chat/remove_message/{messageId}
```

Response message example:

```json
{
  "action": "REMOVE",
  "timestamp": 1712406700.330034700,
  "senderId": 1,
  "chatId": "66054151e162397346b28554",
  "id": "6611400eed7de1513ca11699"
}
```

<br/>

### ❌ Clear chat history

Message structure:

```
path=/chat/{chatId}/clear_history
```

Response message example:

```json
{
  "action": "CLEAR_HISTORY",
  "timestamp": 1712406700.330034700,
  "senderId": 1,
  "chatId": "66054151e162397346b28554"
}
```

<br/>

### 👀 Read messages

Message structure:

```
path=/chat/{chatId}/read

{
  "timestamp": number
}
```

Response message example:
```json
{
  "action": "READ",
  "timestamp": 1711664377.341578700,
  "senderId": 34,
  "chatId": "66054151e162397346b28554",
  "messages": [
    "6605e97a576f710ba44a3dd7",
    "6605ea09576f710ba44a3dd8",
    "6605ea0e576f710ba44a3dd9",
    "6605ea0e576f710ba44a3dda",
    "6605ea0f576f710ba44a3ddb"
  ]
}
```

Possible errors:

- Invalid chat ID

```json
{
  "action": "ERROR",
  "timestamp": 1711664377.341578700,
  "error": "You are not member of chat",
  "path": "/chat/6611450ced7de1513ca1169d/read"
}
```

<br/>

### 📥 Get unread messages

Message structure:

```
path=/chat/unread
```

Response message example:

```json
{
  "action": "UNREAD_MESSAGES",
  "timestamp": 1711704048.542412300,
  "chats": {
    "66054151e162397346b28554": 3,
    "6605584bb6c9df73337beba3": 0
  }
}
```

<br/>

### 📥 Get unread messages per chat

Message structure:

```
path=/chat/unread/{chatId}
```

Response message example:

```json
{
  "action": "UNREAD_MESSAGES",
  "timestamp": 1711704048.542412300,
  "chats": {
    "66054151e162397346b28554": 3
  }
}
```

</details>