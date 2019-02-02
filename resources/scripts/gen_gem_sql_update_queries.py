"""
A python script that maps gems to their toolbar icons
"""

from json import load
from sys import stdout

def printf(format, *args):
  stdout.write(format % args)

data = {}
with open("gemMap.json", "r") as f:
  data = load(f)

for key, item in data.items():
  printf("update data_itemData set icon = \"%s\" where name = \"%s\";\n", item["gem"], key)
  
input("\nPress any key to continue...")
