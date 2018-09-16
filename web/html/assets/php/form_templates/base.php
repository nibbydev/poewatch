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
    <h4>Ilvl</h4>
    <div class="form-group">
      <select class="form-control" id="select-ilvl">
        <option value="all" selected>All</option>
        <option value="68-74">68 - 74</option>
        <option value="75-82">75 - 82</option>
        <option value="83-84">83 - 84</option>
        <option value="85-100">85 - 100</option>
      </select>
    </div>
  </div>
  <div class="col-6 col-md-3 mb-2">
    <h4>Influence</h4>
    <select class="form-control custom-select" id="select-influence">
      <option value="all" selected>All</option>
      <option value="none">None</option>
      <option value="either">Either</option>
      <option value="shaped">Shaper</option>
      <option value="elder">Elder</option>
    </select>
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