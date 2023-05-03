# sha1-fpga

## 怎么跑仿真？（配置环境）

只需要有 sbt 和 java, 版本分别为 1.8.1 和 Temurin 19

然后在项目根目录下运行 `sbt test` 即可

运行该命令可能需要下载 scala 以及 chisel 相关的依赖库，如果是 IDEA 环境确保你设置了 IDEA 的代理，其它的环境也需要设置系统代理，否则可能会下载失败

## 项目结构

``` shell
src
├── main
│ └── scala
│     └── sha1
│         ├── sha1block.scala // 处理 1 个 block 512 bit 的数据
│         ├── sha1core.scala // 处理多个 block 的数据
│         ├── sha1round.scala // 处理 80 个周期
│         ├── sha1shift.scala // 移位寄存器
│         └── sha1top.scala // 接受任意长度字符串，分割和补零成符合 sha1 标准的 block 之后传给 sha1core 处理
└── test
    └── scala
        ├── TestAnnotations.scala
        ├── sha1blockTest.scala
        ├── sha1coreTest.scala
        └── sha1topTest.scala // 验证整个 sha1 算法的正确性，和标准库的 sha1 算法结果对比
```

