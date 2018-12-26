<?php function tierForm() { ?>
  <div class="mr-3 mb-2">
    <h4>Tier</h4>
    <select class="form-control" id="select-tier">
      <option value="all" selected>All</option>
      <option value="none">None</option>
      <option value="1">1</option>
      <option value="2">2</option>
      <option value="3">3</option>
      <option value="4">4</option>
      <option value="5">5</option>
      <option value="6">6</option>
      <option value="7">7</option>
      <option value="8">8</option>
      <option value="9">9</option>
      <option value="10">10</option>
      <option value="11">11</option>
      <option value="12">12</option>
      <option value="13">13</option>
      <option value="14">14</option>
      <option value="15">15</option>
      <option value="16">16</option>
    </select>
  </div>
<?php } ?>

<?php function qualityForm() { ?>
  <div class="mr-3 mb-2">
    <h4>Quality</h4>
    <select class="form-control" id="select-quality">
      <option value="all" selected>All</option>
      <option value="0">0</option>
      <option value="20">20</option>
      <option value="23">23</option>
    </select>
  </div>
<?php } ?>

<?php function levelForm() { ?>
  <div class="mr-3 mb-2">
    <h4>Level</h4>
    <select class="form-control" id="select-level">
      <option value="all" selected>All</option>
      <option value="1">1</option>
      <option value="2">2</option>
      <option value="3">3</option>
      <option value="4">4</option>
      <option value="20">20</option>
      <option value="21">21</option>
    </select>
  </div>
<?php } ?>

<?php function influenceForm() { ?>
  <div class="mr-3 mb-2">
    <h4>Influence</h4>
    <select class="form-control" id="select-influence">
      <option value="all" selected>All</option>
      <option value="none">None</option>
      <option value="either">Either</option>
      <option value="shaper">Shaper</option>
      <option value="elder">Elder</option>
    </select>
  </div>
<?php } ?>

<?php function itemLevelForm() { ?>
  <div class="mr-3 mb-2">
    <h4>Ilvl</h4>
    <select class="form-control" id="select-ilvl">
      <option value="all" selected>All</option>
      <option value="82">82</option>
      <option value="83">83</option>
      <option value="84">84</option>
      <option value="85">85</option>
      <option value="86">86+</option>
    </select>
  </div>
<?php } ?>

<?php function corruptedForm() { ?>
  <div class="mr-3 mb-2">
    <h4>Corrupted</h4>
    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-corrupted">
      <label class="btn btn-outline-dark active">
        <input type="radio" name="corrupted" value="all" checked>Both
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="corrupted" value="true">Yes
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="corrupted" value="false">No
      </label>
    </div>
  </div>
<?php } ?>

<?php function linksForm() { ?>
  <div class="mr-3 mb-2">
    <h4>Links</h4>
    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-links">
      <label class="btn btn-outline-dark active">
        <input type="radio" name="links" value="none" checked=>None
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="links" value="5">5L
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="links" value="6">6L
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="links" value="all">All
      </label>
    </div>
  </div>
<?php } ?>

<?php function rarityForm() { ?>
  <div class="mr-3 mb-2">
    <h4>Rarity</h4>
    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-rarity">
      <label class="btn btn-outline-dark active">
        <input type="radio" name="rarity" value="all" checked>Both
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="rarity" value="unique">Unique
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="rarity" value="relic">Relic
      </label>
    </div>
  </div>
<?php } ?>
