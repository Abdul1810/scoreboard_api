function addNumber(value) {
    const display = document.getElementById("display");
    display.value += value;

    display.dispatchEvent(new Event('input'));
}

function clearDisplay() {
    document.getElementById("display").value = "";
}

function removeLastNumber() {
    var value = document.getElementById("display").value;
    document.getElementById("display").value = value.substring(0, value.length - 1);
}

// function sendRequest(path) {
//     const value = document.getElementById("display").value;
//
//     const params = new URLSearchParams();
//     params.append("num", value);
//
//     // makeRequest(path, params, "POST", (data) => {
//     //     document.getElementById("result").value = data;
//     // });
//
//     makeRequest(path, params, "GET", (data) => {
//         document.getElementById("result").value = data;
//     });
// }

function makeRequest(path, body, method = "GET", onResult) {
    let url = path;

    if (method === "GET" && body) {
        url += "?" + body.toString();
        body = null;
    }

    fetch(url, {
        method: method,
        body: body,
    }).then(response => response.text())
        .then(data => {
            onResult(data);
        })
        .catch(error => {
            console.error("Error:", error);
        });
}