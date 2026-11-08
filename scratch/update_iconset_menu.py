path = r'app/src/main/java/com/stealzalut/mobile/ui/theme/SzlIconSet.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('import androidx.compose.material.icons.outlined.Menu', 'import androidx.compose.material.icons.outlined.Menu\nimport androidx.compose.material.icons.outlined.MoreVert')
c = c.replace('import androidx.compose.material.icons.rounded.Menu', 'import androidx.compose.material.icons.rounded.Menu\nimport androidx.compose.material.icons.rounded.MoreVert')

c = c.replace('override val actionMenu: ImageVector = Icons.Rounded.Menu', 'override val actionMenu: ImageVector = Icons.Rounded.MoreVert')
c = c.replace('override val actionMenu: ImageVector = Icons.Outlined.Menu', 'override val actionMenu: ImageVector = Icons.Outlined.MoreVert')

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

print("Updated SzlIconSet actionMenu to MoreVert")
