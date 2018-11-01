"""
A python script that generates Java methods for BaseNameExtractor.
Sue me.
"""

from urllib.request import urlopen
from json import loads, dump
from math import ceil
from sys import stdout

def printf(format, *args):
  stdout.write(format % args)
  
def genMethod(category, bases):
  longestCharCount = 0
  
  for base in bases:
    if longestCharCount < len(base):
      longestCharCount = len(base)
      
  printf("private static String extract%sBase(String name) {\n", category.capitalize())

  for base in bases:
    printf("    if (name.contains(\"%s\"))%s return \"%s\";\n", base, ' ' * (longestCharCount - len(base)), base)

  print("    return null;\n}\n")

def getBaseData(url, categoryMap):
  data = loads(urlopen(url).read().decode("utf-8"))
  formattedData = {}
  longestCharCount = 0

  for key, item in data.items():
    if item["item_class"] not in categoryMap:
      continue
      
    key = categoryMap[item["item_class"]]

    if key not in formattedData:
      formattedData[key] = []
      
    if key == "amulet" and "Talisman" in item["name"]:
      continue

    if longestCharCount < len(item["name"]):
      longestCharCount = len(item["name"])
      
    formattedData[key].append(item["name"])
    
  return formattedData

categoryMap = {
  'Amulet': 'amulet',
  'Body Armour': 'chest',
  'Boots': 'boots',
  'Gloves': 'gloves',
  'Helmet': 'helmet',
  'Shield': 'shield',
  'Belt': 'belt',
  'AbyssJewel': 'jewel',
  'Jewel': 'jewel',
  'Quiver': 'quiver',
  'Ring': 'ring',
  'Claw': 'claw',
  'Dagger': 'dagger',
  'One Hand Axe': 'oneaxe',
  'One Hand Mace': 'onemace',
  'Sceptre': 'sceptre',
  'One Hand Sword': 'onesword',
  'Thrusting One Hand Sword': 'onesword',
  'Wand': 'wand',
  'FishingRod': 'rod',
  'Bow': 'bow',
  'Staff': 'staff',
  'Two Hand Axe': 'twoaxe',
  'Two Hand Mace': 'twomace',
  'Two Hand Sword': 'twosword'
}

url = "https://github.com/brather1ng/RePoE/raw/master/data/base_items.json"
formattedData = getBaseData(url, categoryMap)

for category, bases in formattedData.items():
  genMethod(category, bases)
  
input("\nPress any key to continue...")
