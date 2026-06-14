const jwt = require('jsonwebtoken');
const token = jwt.sign({ sub: "minhdt", roles: ["ROLE_ADMIN"] }, Buffer.from("RmFzaGlvblN0b3JlU3ByaW5nQm9vdEpXVDIwMjZWZXJ5U3Ryb25nS2V5", "base64"), { expiresIn: '1h' });
console.log(token);
