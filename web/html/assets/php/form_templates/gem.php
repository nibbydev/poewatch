<div class="row mb-3">
  <div class="col-6 col-md-4 mb-2 order-1 order-sm-1 order-md-1 mb-3">
    <h4>Corrupted</h4>
    <div class="btn-group btn-group-toggle" data-toggle="buttons" id="radio-corrupted">
      <label class="btn btn-outline-dark active">
        <input type="radio" name="corrupted" value="all">Both
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="corrupted" value="0">No
      </label>
      <label class="btn btn-outline-dark">
        <input type="radio" name="corrupted" value="1" checked>Yes
      </label>
    </div>
  </div>
  <div class="col-6 col-md-4 mb-2 order-3 order-sm-3 order-md-2">
    <h4>Level</h4>
    <div class="form-group">
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
  </div>
  <div class="col-6 col-md-4 mb-2 order-5 order-sm-5 order-md-3">
    <h4>Quality</h4>
    <div class="form-group">
      <select class="form-control" id="select-quality">
        <option value="all" selected>All</option>
        <option value="0">0</option>
        <option value="20">20</option>
        <option value="23">23</option>
      </select>
    </div>
  </div>
  <div class="col-6 col-md-4 mb-2 order-2 order-sm-2 order-md-4 mb-3">
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
  <div class="col-6 col-md-4 mb-2 order-4 order-sm-4 order-md-5">
    <h4>Category</h4>
    <select class="form-control custom-select" id="search-sub">
    
      <?php AddSubCategorySelectors($SERVICE_categories); ?>

    </select>
  </div>
  <div class="col-6 col-md-4 mb-2 order-6 order-sm-6 order-md-6">
    <h4>Search</h4>
    <input type="text" class="form-control" id="search-searchbar" placeholder="Search">
  </div>
</div>