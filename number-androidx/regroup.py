import re
import pprint

# Read the original file
filepath = '/Users/tayap/project-naming/number-androidx/app/src/main/res/layout/fragment_user_logout_admin.xml'
with open(filepath, 'r', encoding='utf-8') as f:
    orig_content = f.read()

# Extract the entire GridLayout part
match = re.search(r'(<GridLayout[\s\S]*?</GridLayout>)', orig_content)
if not match:
    print("GridLayout not found!")
    exit(1)

grid_content = match.group(1)

# Extract all buttons
buttons_raw = re.findall(r'<com\.google\.android\.material\.button\.MaterialButton[^>]*/>', grid_content)
button_map = {}
for b in buttons_raw:
    # get id
    id_match = re.search(r'android:id="@+id/([^"]+)"', b)
    if id_match:
        button_map[id_match.group(1)] = b

groups = [
    {
        "title": "กลุ่มข้อความพิเศษ",
        "buttons": ["btn_secret_code", "btn_personal_msg"]
    },
    {
        "title": "กลุ่มแสดง VIP",
        "buttons": ["btn_color_bag", "btn_lucky_number", "btn_buddha_pang_assign", "btn_sacred_temple_assign", "btn_spell_assign", "btn_inauspicious_assign"]
    },
    {
        "title": "กลุ่มวิธีการ",
        "buttons": ["btn_merit_assign", "btn_changenum_assign"]
    },
    {
        "title": "กลุ่มสินค้า",
        "buttons": ["btn_tabian_manage", "btn_category_manage", "btn_product_manage", "btn_zircon_orders"]
    },
    {
        "title": "จัดการข้อมูล",
        "buttons": ["btn_admin_chat", "btn_add_dream", "btn_change_vip_status", "btn_guest_address_manage"]
    }
]

new_content = ""
for idx, g in enumerate(groups):
    title = g["title"]
    margin_top = "8dp" if idx == 0 else "16dp"
    
    new_content += f'''            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="{title}"
                android:textSize="18sp"
                android:textStyle="bold"
                android:textColor="#333333"
                android:layout_marginStart="12dp"
                android:layout_marginEnd="12dp"
                android:layout_marginTop="{margin_top}"
                android:layout_marginBottom="8dp" />

            <GridLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:layout_marginEnd="8dp"
                android:columnCount="2"
                android:alignmentMode="alignMargins"
                android:columnOrderPreserved="false">
'''
    for bid in g["buttons"]:
        if bid in button_map:
            # indent nicely
            btn_lines = button_map[bid].split('\n')
            formatted_btn = "\n".join(["                " + line.strip() if line.strip() else "" for line in btn_lines]).strip()
            new_content += f'\n                {formatted_btn}\n'
    new_content += '            </GridLayout>\n\n'

# remove trailing newline
new_content = new_content.rstrip()

# Now find exact lines to replace using replace_file_content structure or write a modified file
final_content = orig_content.replace(grid_content, new_content)
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(final_content)

print("Done grouping!")
