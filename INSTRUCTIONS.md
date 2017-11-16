Server Setup
===
You should setup the kurento media server and the kurento application server before running the client.

### Kurento Media Server
Follow the instruction in https://github.com/johnson-li/kurento-server

### Kurento Application Server
Follow the instruction in https://github.com/johnson-li/kurento-application-server


Parameters Configuration
===
Change the value of DEFAULT_SERVER in Constants.java, modify the address to the IP address of your kurento application server.

Note
===
The IP address of kurento media server that configured in the kurento application server should be accessible from your mobile device. Because the mobile device will communicate with the kurento media server directly.

Install
===
Run `./gradlew installDebug` in the root directory.

User Guide
===
