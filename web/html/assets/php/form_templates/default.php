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
  <div class="col-6 col-md-3 mb-2 offset-md-3">
    <h4>Category</h4>
    <select class="form-control custom-select" id="search-sub">
    
      <?php AddSubCategorySelectors($SERVICE_categories); ?>
    
    </select>
  </div>
  <div class="col-6 col-md-3 mb-2 offset-md-0 offset-6">
    <h4>Search</h4>
    <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
  </div>
</div>