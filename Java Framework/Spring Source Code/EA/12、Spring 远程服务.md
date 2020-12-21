# 12 远程服务

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

RMI就暂时不看了
&emsp;&emsp;Java远程方法调用，即Java RMI(Java Remote Method Invocation)， 是 Java 变成语言里一种用于实现远程调用过程的引用程序编码接口。它使客户机上运行的程序可以调用远程服务器上的对象。远程方法调用特性使Java变成人员能够在网络环境中分布操作。RMI全部的宗旨就是尽可能地简化远程接口对象的使用。
&emsp;&emsp;Java RMI 极大地依赖于接口。在需要创建一个远程对象时，程序员通过传递一个接口来隐藏底层的实现细节。客户端得到的远程对象句柄正好与本地的根代码连接，由后者负责透过网络通信。这样依赖，程序员只需要关心如何通过自己的接口句柄发送信息。
## 12.1 RMI
&emsp;&emsp;在Spring中，同样提供了对RMI的支持，使得在 Spring 下的RMI开发变得更方便，同样，我们还是通过示例来快速体验RMI所提供的功能。
### 12.1.1 示例
&emsp;&emsp;以下提供Spring整合RMI的示例。
1. 建立RMI对外接口。
 ```java
 ```