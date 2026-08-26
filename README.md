```markdown
# MCA: Romantic Expansion 更新日志

## 版本 1.0.0-primary-26.2


### 🎉 新增内容

#### 婚服系统

**男女分款婚服**
- **男性款**：粗手臂设计（4 格宽），展现阳刚之气
- **女性款**：细手臂设计（3 格宽），展现柔美线条

**多文化婚服支持**
| 文化 | 纹理 ID |
|------|---------|
| 🇨🇳 中式婚服 | `chinese_wedding_male/female` |
| 🇪🇺 西式婚服 | `western_wedding_male/female` |
| 🌍 东非婚服 | `east_african_wedding_male/female` |
| 🌍 西非婚服 | `west_african_wedding_male/female` |
| 🏛️ 古希腊婚服 | `ancient_greek_wedding_male/female` |
| 🇯🇵 日式婚服 | `japanese_wedding_male/female` |
| 🇩🇪 德国婚服 | `german_wedding_male/female` |
| 🏴󠁧󠁢󠁳󠁣󠁴󠁿 苏格兰婚服 | `scottish_wedding_male/female` |
| 🇷🇺 斯拉夫婚服 | `slavic_wedding_male/female` |

**Curios 饰品集成**
- 婚服可通过 Curios 槽位穿戴
- 不影响盔甲装备
- 支持饰品渲染


### 🛠️ 优化与改进

- **性别系统优化**：增强性别数据读取的稳定性和缓存机制
- **怀孕系统完善**：优化怀孕检测、概率计算和周期管理
- **代码重构**：清理冗余代码，提升模组性能


### ❌ 移除内容

#### 公主抱系统（技术原因暂时移除）

- **原因**：MC 26.2 版本底层骑乘机制变动，公主抱功能暂时无法稳定运行
- **移除内容**：
  - 相关 Mixin 类
  - 网络包（Packet）
  - 客户端渲染代码
- **未来计划**：将在后续版本中重新实现


### 📝 已知问题

- 婚服纹理文件需要玩家自行准备（`textures/armor/*.png`）


### 🔮 未来计划

- 公主抱系统重新设计
- 更多文化风格的婚服
- 婚礼仪式完整流程
```