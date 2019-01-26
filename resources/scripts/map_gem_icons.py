"""
A python script that maps gems to their toolbar icons
"""

from urllib.request import urlopen
from json import loads, dump
from sys import stdout

def printf(format, *args):
  stdout.write(format % args)
  
def getGemList(url):
  data = loads(urlopen(url).read().decode("utf-8"))
  gemList = []
  
  for item in data:
    if item["group"] == "support":
      continue
    if item["name"] not in gemList:
      gemList.append(item["name"])
  
  return gemList

def genGemMap(gemList):
  gemMap = {}
  count = 0
  
  for gem in gemList:
    count += 1
    url = "https://poedb.tw/us/hover.php?t=gem&n=" + gem.replace(" ", "+")
    htmlReply = urlopen(url).read().decode("utf-8")
    
    # Find indexes of urls in html reply
    URL1_lower = htmlReply.find("https://")
    URL1_upper = htmlReply.find("\"", URL1_lower)
    URL2_lower = htmlReply.find("https://", URL1_upper)
    URL2_upper = htmlReply.find("\"", URL2_lower)
    
    # Extract urls from reply
    gemMap[gem] = {
      "gem": htmlReply[URL1_lower:URL1_upper],
      "bar": htmlReply[URL2_lower:URL2_upper]
    }
    
    printf("[%d/%d] %s\n", count, len(gemList), gem)

  return gemMap

gemList = getGemList("https://api.poe.watch/itemdata?category=gem")
gemMap = genGemMap(gemList)

print(gemMap)

with open("gemMap.json", 'w') as f:
  dump(gemMap, f)

input("\nPress any key to continue...")
