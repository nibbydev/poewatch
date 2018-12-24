$("button.pagination-btn").click(function(){
  $(".pagination-page").addClass("d-none");
  $(".pagination-btn").removeClass("active");
  $("#page-" + $(this).val()).removeClass("d-none");
  $(this).addClass("active")
});
