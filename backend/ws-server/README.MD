## Web-socket routes documentation

### 1. Create a group chat

Message structure:

```
path=/chat/group/create

{
  "name": "Dark side",
  "description": null,
  "isPrivate": false
}
```

Response:

```json
{
  "id": "65f9866179e9dc3991b6ee4d",
  "createdAt": 1710851681.104280700,
  "name": "Dark side",
  "avatarPath": null
}
```

Otherwise:

```json
{
  "action": "ERROR",
  "timestamp": 1710625976.329068800,
  "error": "Requested chat not found",
  "path": null
}
```

<br/>

### 2. Join in the group chat

Message structure:

```
path=/chat/group/{chatId}/join
```

Response message structure:

```json
{
  "action": "JOIN",
  "timestamp": 1710622097.792737500,
  "messageId": null,
  "chatId": "65f605253b9b7c0d6dab5546",
  "senderId": 34,
  "text": null
}
```

<br/>

### 3. Leave from the group chat

Message structure:

```
path=/chat/group/{chatId}/leave
```

Response message is absent if you were a member of the chat.
In other case you will receive message with `ERROR` action:

```json
{
  "action": "ERROR",
  "timestamp": 1710622280.309209600,
  "error": "You are not member of chat",
  "path": null
}
```

<br/>

### 4. Send a text-message to the group chat

Message structure:

```
path=/chat/65f605253b9b7c0d6dab5546/send

{
  "tmpMessageId": "3ba88cab-8ca5-48e7-aff4-ba689fccee70",
  "text": "Hi everyone!"
}
```

At first, you receive the bind message:

```json
{
  "tmpMessageId": "3ba88cab-8ca5-48e7-aff4-ba689fccee70",
  "messageId": "65f607523b9b7c0d6dab5547"
}
```

And then every member of the destination chat will receive your message:

```json
{
  "action": "MESSAGE",
  "timestamp": 1710622546.515123400,
  "messageId": "65f607523b9b7c0d6dab5547",
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
  "path": null
}
```

<br/>