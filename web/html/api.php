<?php
require_once "assets/php/pageData.php";
require_once "assets/php/templates/body.php";

$PAGE_DATA["title"] = "API - PoeWatch";
$PAGE_DATA["pageHeader"] = "API Resources";
$PAGE_DATA["description"] = "Resources for developers";

// page data
$APIColumns = [
  // id api
  [
    "href" => "https://api.poe.watch/id",
    "name" => "id",
    "desc" => "Latest change ID from the top of the river and the time it was fetched.",
    "request" => null,
    "reply" => [
      [
        "param" => "id",
        "condition" => null,
        "type" => "string",
        "desc" => "The change ID"
      ],
      [
        "param" => "time",
        "condition" => null,
        "type" => "ISO 8601 UTC time",
        "desc" => "Time the change ID was fetched"
      ],
    ]
  ],
  // league api
  [
    "href" => "https://api.poe.watch/leagues",
    "name" => "leagues",
    "desc" => "List of current leagues. Entries are sorted such that event leagues appear first, 
      followed by the challenge leagues and then the permanent leagues. SSF and private leagues are omitted.",
    "request" => null,
    "reply" => [
      [
        "param" => "id",
        "condition" => null,
        "type" => "uint",
        "desc" => "Unique ID of the league"
      ],
      [
        "param" => "name",
        "condition" => null,
        "type" => "string",
        "desc" => "Unique name of the league (as it appears in the official API)"
      ],
      [
        "param" => "display",
        "condition" => "If available",
        "type" => "string",
        "desc" => "Formatted league name for display, usually shortened"
      ],
      [
        "param" => "hardcore",
        "condition" => null,
        "type" => "bool",
        "desc" => "The league is hardcore"
      ],
      [
        "param" => "upcoming",
        "condition" => null,
        "type" => "bool",
        "desc" => "The league has not started yet"
      ],
      [
        "param" => "active",
        "condition" => null,
        "type" => "bool",
        "desc" => "The league is currently ongoing"
      ],
      [
        "param" => "event",
        "condition" => null,
        "type" => "bool",
        "desc" => "The league is an event league (eg Flashback)"
      ],
      [
        "param" => "challenge",
        "condition" => null,
        "type" => "bool",
        "desc" => "The league is a challenge league (eg Incursion)"
      ],
      [
        "param" => "start",
        "condition" => "If available",
        "type" => "ISO 8601 UTC time",
        "desc" => "Start date of the league"
      ],
      [
        "param" => "end",
        "condition" => "If available",
        "type" => "ISO 8601 UTC time",
        "desc" => "End date of the league"
      ],
    ]
  ],
  // item data api
  [
    "href" => "https://api.poe.watch/itemdata",
    "name" => "itemdata",
    "desc" => "All items found in the stash API and their defining properties.",
    "request" => null,
    "reply" => [
      [
        "param" => "id",
        "alwaysPresent" => true,
        "condition" => null,
        "type" => "uint",
        "desc" => "Unique ID"
      ],
      [
        "param" => "name",
        "alwaysPresent" => true,
        "condition" => null,
        "type" => "string",
        "desc" => "Name of the item"
      ],
      [
        "param" => "type",
        "alwaysPresent" => false,
        "condition" => "Situational",
        "type" => "string",
        "desc" => "Additional base type field"
      ],
      [
        "param" => "category",
        "alwaysPresent" => true,
        "condition" => null,
        "type" => "string",
        "desc" => "Primary category the item belongs to. Categories can be found under the category api."
      ],
      [
        "param" => "group",
        "alwaysPresent" => true,
        "condition" => null,
        "type" => "string",
        "desc" => "Secondary category the item belongs to. Groups can be found under the category api."
      ],
      [
        "param" => "frame",
        "alwaysPresent" => true,
        "condition" => null,
        "type" => "uint",
        "desc" => "Numeric representation of the item's rarity (eg normal/unique). Same as official usage."
      ],

      [
        "param" => "mapSeries",
        "alwaysPresent" => false,
        "condition" => "category=map",
        "type" => "uint | null",
        "desc" => "5 for Synthesis, 1 for Awakening, etc"
      ],
      [
        "param" => "mapTier",
        "alwaysPresent" => false,
        "condition" => "category=map",
        "type" => "uint | null",
        "desc" => "Tier of map, if applicable"
      ],

      [
        "param" => "baseIsShaper",
        "alwaysPresent" => false,
        "condition" => "category=base",
        "type" => "bool",
        "desc" => "The base has shaper influence"
      ],
      [
        "param" => "baseIsElder",
        "alwaysPresent" => false,
        "condition" => "category=base",
        "type" => "bool",
        "desc" => "The base has elder influence"
      ],
      [
        "param" => "baseItemLevel",
        "alwaysPresent" => false,
        "condition" => "category=base",
        "type" => "uint",
        "desc" => "Item level of base"
      ],

      [
        "param" => "gemLevel",
        "alwaysPresent" => false,
        "condition" => "category=gem",
        "type" => "uint",
        "desc" => "Level of gem (1-21)"
      ],
      [
        "param" => "gemQuality",
        "alwaysPresent" => false,
        "condition" => "category=gem",
        "type" => "uint",
        "desc" => "Quality of gem (0-23)"
      ],
      [
        "param" => "gemIsCorrupted",
        "alwaysPresent" => false,
        "condition" => "category=gem",
        "type" => "bool",
        "desc" => "The gem is corrupted"
      ],

      [
        "param" => "enchantMin",
        "alwaysPresent" => false,
        "condition" => "category=enchantment",
        "type" => "float | null",
        "desc" => "Enchantment's minimum has numeric value"
      ],
      [
        "param" => "enchantMax",
        "alwaysPresent" => false,
        "condition" => "category=enchantment",
        "type" => "float | null",
        "desc" => "Enchantment's maximum has numeric value"
      ],

      [
        "param" => "stackSize",
        "alwaysPresent" => false,
        "condition" => "Item is stackable",
        "type" => "uint | null",
        "desc" => "Default stack size of item type"
      ],
      [
        "param" => "linkCount",
        "alwaysPresent" => false,
        "condition" => "Item has significant links",
        "type" => "uint | null",
        "desc" => "For weapons/armour only (5 or 6)"
      ],

      [
        "param" => "variation",
        "alwaysPresent" => false,
        "condition" => "There are multiple instances of the same item but with different properties",
        "type" => "string",
        "desc" => "Certain items (eg Vessel of Vinktar or Doryanis Invitation) tend to have variations, this field is to tell them apart."
      ],
      [
        "param" => "icon",
        "alwaysPresent" => true,
        "condition" => null,
        "type" => "string",
        "desc" => "Icon of the item"
      ],
    ]
  ],
  // characters api
  [
    "href" => "https://api.poe.watch/characters?account=novynn",
    "name" => "characters",
    "desc" => "Get player character names found through the stash API. 
      If a player has listed an item in a public stash tab, that character name is recorded.",
    "request" => [
      [
        "param" => "account",
        "required" => true,
        "desc" => "Case insensitive account name"
      ]
    ],
    "reply" => [
      [
        "param" => "character",
        "condition" => null,
        "type" => "string",
        "desc" => "Character name"
      ],
      [
        "param" => "league",
        "condition" => null,
        "type" => "string",
        "desc" => "In league"
      ],
      [
        "param" => "found",
        "condition" => null,
        "type" => "ISO 8601 UTC time",
        "desc" => "Time the character was first found"
      ],
      [
        "param" => "seen",
        "condition" => null,
        "type" => "ISO 8601 UTC time",
        "desc" => "Time the character was last seen"
      ],
    ]
  ],
  // accounts api
  [
    "href" => "https://api.poe.watch/accounts?character=quillhitman",
    "name" => "accounts",
    "desc" => "Get player account names found through the stash API. 
      If a player has listed an item in a public stash tab, that account name is recorded.",
    "request" => [
      [
        "param" => "character",
        "required" => true,
        "desc" => "Case insensitive character name"
      ]
    ],
    "reply" => [
      [
        "param" => "account",
        "condition" => null,
        "type" => "string",
        "desc" => "Account name"
      ],
      [
        "param" => "found",
        "condition" => null,
        "type" => "ISO 8601 UTC time",
        "desc" => "Time the account was first found"
      ],
      [
        "param" => "seen",
        "condition" => null,
        "type" => "ISO 8601 UTC time",
        "desc" => "Time the account was last seen"
      ],
    ]
  ],
  // categories api
  [
    "href" => "https://api.poe.watch/categories",
    "name" => "categories",
    "desc" => "List of categories and groups currently in use.",
    "request" => null,
    "reply" => [
      [
        "param" => "id",
        "condition" => null,
        "type" => "uint",
        "desc" => "Unique id of the category"
      ],
      [
        "param" => "name",
        "condition" => null,
        "type" => "string",
        "desc" => "Unique name of the category"
      ],
      [
        "param" => "display",
        "condition" => "If set",
        "type" => "string | null",
        "desc" => "Display name of the category"
      ],
      [
        "param" => "groups",
        "condition" => null,
        "type" => "list (group)",
        "desc" => "List of groups associated with the category. Named separately from categories."
      ],
    ]
  ],
  // get api
  [
    "href" => "https://api.poe.watch/get?league=Standard&category=flask",
    "name" => "get",
    "desc" => "Returns price and item data for specified league and category. 
      Items are listed in decreasing order from most expensive to least expensive. 
      Updated every 10 minutes. Capitalization does not matter for request fields.",
    "request" => [
      [
        "param" => "league",
        "required" => true,
        "desc" => "League name"
      ],
      [
        "param" => "category",
        "required" => true,
        "desc" => "Category name (see category API)"
      ],
    ],
    "reply" => [
      [
        "param" => "...",
        "condition" => null,
        "type" => "...",
        "desc" => "< all parameters exactly from itemdata api >"
      ],
      [
        "param" => "...",
        "condition" => null,
        "type" => "...",
        "desc" => "< all parameters exactly from compact api >"
      ],
      [
        "param" => "change",
        "condition" => null,
        "type" => "float",
        "desc" => "Price compared to 7 days ago as percentage"
      ],
      [
        "param" => "history",
        "condition" => null,
        "type" => "list (float)",
        "desc" => "Mean prices from last 7 days. Last element is current mean."
      ]
    ]
  ],
  // item api
  [
    "href" => "https://api.poe.watch/item?id=259",
    "name" => "item",
    "desc" => "Retrieves information about a specific item.",
    "request" => [
      [
        "param" => "id",
        "required" => true,
        "desc" => "Numeric id of an item"
      ],
    ],
    "reply" => [
      [
        "param" => "...",
        "condition" => null,
        "type" => "...",
        "desc" => "< all parameters exactly from itemdata api >"
      ],
      [
        "param" => "leagues",
        "condition" => null,
        "type" => "list",
        "desc" => "List of leagues the item has appeared in and its last known prices"
      ]
    ]
  ],
  // item history api
  [
    "href" => "https://api.poe.watch/itemhistory?id=142&league=Synthesis",
    "name" => "itemhistory",
    "desc" => "Finds prices from past leagues. Use item api to find list of applicable leagues.",
    "request" => [
      [
        "param" => "id",
        "required" => true,
        "desc" => "Item ID"
      ],
      [
        "param" => "league",
        "required" => true,
        "desc" => "League name"
      ],
    ],
    "reply" => [
      [
        "param" => "time",
        "condition" => null,
        "type" => "ISO 8601 UTC time",
        "desc" => "Time the price was collected"
      ],
      [
        "param" => "mean",
        "condition" => null,
        "type" => "float",
        "desc" => "Mean average price"
      ],
      [
        "param" => "median",
        "condition" => null,
        "type" => "float",
        "desc" => "Median average price"
      ],
      [
        "param" => "mode",
        "condition" => null,
        "type" => "float",
        "desc" => "Mode average price"
      ],
      [
        "param" => "daily",
        "condition" => null,
        "type" => "uint",
        "desc" => "Nr of items found per 24h"
      ],
      [
        "param" => "current",
        "condition" => null,
        "type" => "uint",
        "desc" => "Nr of items currently on sale"
      ],
      [
        "param" => "accepted",
        "condition" => null,
        "type" => "uint",
        "desc" => "Nr of items accepted for price calculation"
      ],
    ]
  ],
  // compact api
  [
    "href" => "https://api.poe.watch/compact?league=Standard",
    "name" => "compact",
    "desc" => "Return price data (id, mean, median, mode, min, max, total, daily, exalted) 
      of all items of the provided active league. IDs can be found in itemdata API described above.",
    "request" => [
      [
        "param" => "league",
        "required" => true,
        "desc" => "Valid league name"
      ]
    ],
    "reply" => [
      [
        "param" => "id",
        "condition" => null,
        "type" => "uint",
        "desc" => "Unique id of the item (see itemdata api)"
      ],
      [
        "param" => "mean",
        "condition" => null,
        "type" => "float",
        "desc" => "Mean average price"
      ],
      [
        "param" => "median",
        "condition" => null,
        "type" => "float",
        "desc" => "Median average price"
      ],
      [
        "param" => "mode",
        "condition" => null,
        "type" => "float",
        "desc" => "Mode average price"
      ],
      [
        "param" => "min",
        "condition" => null,
        "type" => "float",
        "desc" => "Min accepted average price"
      ],
      [
        "param" => "max",
        "condition" => null,
        "type" => "float",
        "desc" => "Max accepted average price"
      ],
      [
        "param" => "exalted",
        "condition" => null,
        "type" => "float",
        "desc" => "Mean price in exalted"
      ],
      [
        "param" => "total",
        "condition" => null,
        "type" => "uint",
        "desc" => "Total nr of items found"
      ],
      [
        "param" => "daily",
        "condition" => null,
        "type" => "uint",
        "desc" => "Nr of items found per 24h"
      ],
      [
        "param" => "current",
        "condition" => null,
        "type" => "uint",
        "desc" => "Nr of items currently on sale"
      ],
      [
        "param" => "accepted",
        "condition" => null,
        "type" => "uint",
        "desc" => "Nr of items accepted for price calculation"
      ],
    ]
  ],
  // listings api
  [
    "href" => "https://api.poe.watch/listings?league=Standard&account=Novynn",
    "name" => "listings",
    "desc" => "Get all item listings for an account, including the time listed, last updated, 
      how many are listed and how many are priced. Allows filtering out items without a 
      price. Only tracks items that are available through the itemdata api.",
    "request" => [
      [
        "param" => "league",
        "required" => true,
        "desc" => "League name"
      ],
      [
        "param" => "account",
        "required" => true,
        "desc" => "Case sensitive account name"
      ],
      [
        "param" => "onlyPriced",
        "required" => false,
        "desc" => "If present, listings with no price will be excluded"
      ],
    ],
    "reply" => [
      [
        "param" => "id",
        "condition" => null,
        "type" => "uint",
        "desc" => "ID of item"
      ],
      [
        "param" => "discovered",
        "condition" => null,
        "type" => "ISO 8601 UTC time",
        "desc" => "Time the item was discovered"
      ],
      [
        "param" => "updated",
        "condition" => null,
        "type" => "ISO 8601 UTC time",
        "desc" => "Time the item was last updated"
      ],
      [
        "param" => "count",
        "condition" => null,
        "type" => "uint",
        "desc" => "How many of the item is the user selling"
      ],
      [
        "param" => "buyout",
        "condition" => null,
        "type" => "list (buyout)",
        "desc" => "Price listings for the item"
      ],
    ]
  ],
];

include "assets/php/templates/header.php";
include "assets/php/templates/navbar.php";
include "assets/php/templates/priceNav.php";
genBodyHeader();
?>
<?php foreach ($APIColumns as $API) { ?>
  <div class="col-12">
    <div class="card custom-card mb-3">
      <div class="card-header">
        <h3 class="card-title nowrap mb-0">
          <a href="<?php echo $API["href"] ?>" target="_blank"><?php echo $API["name"] ?></a>
        </h3>
      </div>
      <div class="card-body px-3 py-2">
        <h5>Description</h5>
        <p class="subtext-0">
          <?php echo $API["desc"] ?>
        </p>
      </div>

      <?php if ($API["request"]) { ?>
        <div class="mb-3">
          <h5 class="px-3">Request parameters</h5>
          <div class="card-body px-3 py-2 api-data-table overflow-hidden">
            <table class="table table-sm mb-0">
              <thead>
              <tr>
                <th>Param</th>
                <th>Required</th>
                <th class="w-100">Description</th>
              </tr>
              </thead>
              <tbody class="subtext-0">
              <?php foreach ($API["request"] as $apiReply) { ?>
                <tr>
                  <td><?php echo $apiReply["param"] ?></td>
                  <td><?php echo $apiReply["required"] ? "Yes" : "No" ?></td>
                  <td><?php echo $apiReply["desc"] ?></td>
                </tr>
              <?php } ?>
              </tbody>
            </table>
          </div>
        </div>
      <?php } ?>

      <?php if ($API["reply"]) { ?>
        <div>
          <h5 class="px-3">Reply fields</h5>
          <div class="card-body px-3 py-2 api-data-table overflow-hidden">
            <table class="table table-sm mb-0">
              <thead>
              <tr>
                <th>Param</th>
                <th>Type</th>
                <th>Condition</th>
                <th>Description</th>
              </tr>
              </thead>
              <tbody class="subtext-0">
              <?php foreach ($API["reply"] as $apiReply) { ?>
                <tr>
                  <td><?php echo $apiReply["param"] ?></td>
                  <td class="nowrap"><?php echo $apiReply["type"] ?></td>
                  <td><?php echo $apiReply["condition"] ?? "-" ?></td>
                  <td><?php echo $apiReply["desc"] ?></td>
                </tr>
              <?php } ?>
              </tbody>
            </table>
          </div>
        </div>
      <?php } ?>
      <div class="card-footer slim-card-edge"></div>
    </div>
  </div>
<?php } ?>

<?php
genBodyFooter();
include "assets/php/templates/footer.php"
?>
