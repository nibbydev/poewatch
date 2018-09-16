<div class="row mb-3">
  <div class="col-6 col-md-3 mb-2">
    <h4 class='nowrap'>Low count</h4>
    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-confidence">
      <label class="btn btn-outline-dark active">
        <input type="radio" name="confidence" value="0" checked><a>Hide</a>
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="confidence" value="1"><a>Show</a>
      </label>
    </div>
  </div>
  <div class="col-6 col-md-3 mb-2 link-fields">
    <h4>Tier</h4>
    <div class="form-group">
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
  </div>
  <div class="col-6 col-md-3 mb-2">
    <h4>Category</h4>
    <select class="form-control custom-select" id="search-sub">

      <?php AddSubCategorySelectors($SERVICE_categories); ?>

    </select>
  </div>
  <div class="col-6 col-md-3 mb-2">
    <h4>Search</h4>
    <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
  </div>
</div>