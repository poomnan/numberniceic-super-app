def generate():
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
    
    # Read the original xml
    with open('/Users/tayap/project-naming/number-androidx/app/src/main/res/layout/fragment_user_logout_admin.xml', 'r') as f:
        lines = f.readlines()
        
    start_idx = 0
    end_idx = 0
    for i, line in enumerate(lines):
        if '<GridLayout' in line and start_idx == 0:
            start_idx = i
        if '</GridLayout>' in line:
            end_idx = i
            break
            
    grid_content = "".join(lines[start_idx:end_idx+1])
    
    # Extract buttons
    import re
    buttons_raw = re.findall(r'<com\.google\.android\.material\.button\.MaterialButton[\s\S]*?/>', grid_content)
    
    button_map = {}
    for b in buttons_raw:
        m = re.search(r'android:id="@+id/([^"]+)"', b)
        if m:
            id = m.group(1)
            button_map[id] = b
            
    new_xml = ""
    for idx, g in enumerate(groups):
        title = g["title"]
        # Add title
        new_xml += f'''
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="{title}"
                android:textSize="18sp"
                android:textStyle="bold"
                android:textColor="#333333"
                android:layout_marginStart="12dp"
                android:layout_marginEnd="12dp"
                android:layout_marginTop="{"16dp" if idx > 0 else "8dp"}"
                android:layout_marginBottom="8dp" />
'''
        # Add grid layout
        new_xml += '''
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
                new_xml += "\n" + "\n".join(["                " + l.strip() if l.strip() else "" for l in button_map[bid].split("\n")]) + "\n"
        
        new_xml += '''
            </GridLayout>
'''

    # Write output
    with open('tmp_new_grid.xml', 'w') as f:
        f.write(new_xml)
        
generate()
