# 小米温湿度2 Android App (MiThermoBLE)

一个用于连接小米温湿度计2（LYWSD03MMC）的安卓应用，支持蓝牙BLE连接、实时数据读取和历史数据查看。

## 功能特性

- 🔍 **设备扫描** - 自动发现附近的小米温湿度计2设备
- 📡 **BLE连接** - 通过蓝牙低功耗连接设备
- 🌡️ **实时数据** - 实时显示温度和湿度数据
- 🔋 **电池状态** - 显示设备电池电量
- 📊 **历史数据** - 从设备读取历史温湿度记录
- 💾 **本地存储** - 数据自动保存到本地数据库
- 🎨 **Material Design 3** - 现代化UI设计，支持动态颜色

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI框架 | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + Clean Architecture |
| BLE | Android原生 BluetoothGatt |
| 数据库 | Room |
| 依赖注入 | Hilt |
| 构建 | Gradle Kotlin DSL + Version Catalog |

## 项目结构

```
app/src/main/java/com/example/mithermoble/
├── MainActivity.kt              # 主Activity
├── MiThermoApp.kt               # Application类
├── data/
│   ├── ble/                     # BLE通信层
│   │   ├── BleManager.kt        # BLE管理器
│   │   ├── BleService.kt        # 前台服务
│   │   └── MiThermoConstants.kt # BLE常量
│   ├── db/                      # 数据库层
│   │   ├── AppDatabase.kt       # Room数据库
│   │   ├── SensorDataDao.kt     # DAO接口
│   │   ├── SensorDataEntity.kt  # 数据实体
│   │   └── Mappers.kt           # 数据映射
│   └── repository/              # 仓库实现
│       └── SensorRepositoryImpl.kt
├── di/                          # 依赖注入
│   ├── BleModule.kt
│   └── DatabaseModule.kt
├── domain/
│   ├── model/Models.kt          # 领域模型
│   └── repository/
│       └── SensorRepository.kt  # 仓库接口
└── ui/
    ├── MainViewModel.kt         # 主ViewModel
    ├── screens/MainScreen.kt    # 主界面
    └── theme/                   # 主题
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## 编译

### 本地编译

确保已安装：
- Android Studio
- JDK 17
- Android SDK (compileSdk 35)

```bash
# 生成Gradle Wrapper
gradle wrapper

# 编译Debug APK
./gradlew assembleDebug

# 编译Release APK
./gradlew assembleRelease
```

### GitHub Actions

推送到 `main` 或 `develop` 分支会自动触发CI编译，生成的APK可在 Actions > Artifacts 中下载。

## 使用方法

1. 打开应用
2. 授予蓝牙和位置权限
3. 点击扫描按钮搜索设备
4. 从列表中选择设备连接
5. 连接成功后查看实时温湿度数据
6. 点击"读取设备历史数据"获取历史记录

## 支持设备

- 小米温湿度计2 (LYWSD03MMC)
- 刷入ATC固件的设备

## 系统要求

- Android 6.0 (API 23) 及以上
- 支持BLE 4.2的设备

## 许可证

MIT License
