import os
import re

dir_path = "app/src/main/res/layout"

count = 0
for filename in os.listdir(dir_path):
    if not filename.endswith(".xml") or filename == "layout_patient_bottom_nav.xml":
        continue
    
    filepath = os.path.join(dir_path, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    # We want to replace the LinearLayout that contains the 4 bottom nav buttons.
    # Patient layouts have navHome, navHistory, etc.
    if "navHome" in content and ("navHistory" in content or "navBook" in content):
        
        # We need to find the <LinearLayout block defining the bottomNav.
        # It's typically at the very end of the file or has id `@+id/bottomNav`.
        # Let's search using a sophisticated matching logic.
        
        start_idx = content.find('<LinearLayout')
        matched_start = -1
        while start_idx != -1:
            end_idx = content.find('>', start_idx)
            header = content[start_idx:end_idx]
            if 'id="@+id/navHome"' in content[start_idx:]:
                if '@+id/bottomNav' in header or '@+id/llBottomNav' in header:
                    matched_start = start_idx
                    break
            start_idx = content.find('<LinearLayout', start_idx + 1)
            
        # fallback: if no id bottomnav, look for the parent containing navHome and navHistory
        if matched_start == -1:
            # use regex to match from <LinearLayout to include navHome
            start_indices = [m.start() for m in re.finditer(r'<LinearLayout', content)]
            for i in start_indices:
                end_of_tag = content.find('>', i)
                tag_content = content[i:end_of_tag+1]
                # is this the master container? Check if navHome and navHistory are inside it
                # basically a linear layout with an id that is mostly just bottom nav
                # or find the last <LinearLayout before navHome that is not closed before navHome
                pass
                
        # actually, many of them have `android:id="@+id/bottomNav"` verbatim.
        # Let's just do regex matching the entire file to find <LinearLayout up to </LinearLayout> that contains navHome, navBook, etc
        
        # find index of navHome
        nav_home_idx = content.find('"@+id/navHome"')
        if nav_home_idx != -1:
            # trace back to the parent LinearLayout
            sub_content = content[:nav_home_idx]
            last_linear = sub_content.rfind('<LinearLayout')
            parent_id_idx = sub_content.rfind('<LinearLayout', 0, last_linear)
            # wait, it's easier: parse the XML.
            
import xml.etree.ElementTree as ET
# Python ElementTree strips comments, so rewrites might destroy formatting.
