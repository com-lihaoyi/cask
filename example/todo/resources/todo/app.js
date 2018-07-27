var state = "all";
var newTodoInput = document.getElementsByClassName("new-todo")[0];
var todoList = document.getElementsByClassName("todo-list")[0];
newTodoInput.addEventListener(
    "keydown",
    function(evt){
        if (evt.keyCode === 13) {
            fetch("/add/" + state, {
                method: "POST",
                body: newTodoInput.value
            })
            .then(function(response){ return response.text()})
            .then(function (text) {
                newTodoInput.value = "";
                todoList.innerHTML = text;
            })
        }
    }
);
newTodoInput.addEventListener(
    "mousedown",
    function(evt){
        if (evt.keyCode === 13) {
            fetch("/add/" + state, {
                method: "POST",
                body: newTodoInput.value
            })
            .then(function(response){ return response.text()})
            .then(function (text) {
                newTodoInput.value = "";
                todoList.innerHTML = text;
            })
        }
    }
);