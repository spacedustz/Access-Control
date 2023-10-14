## 📘 style.css

1개의 CSS를 2개의 HTML에서 사용하고 있습니다.

```css
/* Button */  
button {  
    background-color: #4CAF50;  
    color: white;  
    padding: 10px 20px;  
    border: none;  
    border-radius: 4px;  
    cursor: pointer;  
}  
  
button:hover {  
    background-color: #45a049;  
}  
  
body {  
    background-color: #001228;  
    /*background-image: url(back.png);*/  
    display: flex;  
    flex-direction: column;  
    align-items: center; /* 수직 정렬 (가운데) */  
    justify-content: center; /* 수평 정렬 (가운데) */  
    height: 100vh; /* 화면 높이에 맞추어 정렬 */    
    overflow: hidden;  
}  
  
/* Input */  
input[type="text"],  
input[type="number"] {  
    padding: 8px;  
    border-radius: 4px;  
}  
  
section {  
    text-align: center;  
}  
  
/* Div */  
div {  
    text-align: center;  
    margin-bottom: .8rem;  
    padding: .8rem;  
    border-radius: .3rem;  
    box-shadow: .1rem .1rem .3rem rgba(0, 0, 0, .2);  
}  
  
/* Span */  
span {  
    font-weight: bold  
}  
  
/* Paragraph */  
p {  
    font-size: 18px  
}  
  
.admin-body {  
    background-color: #001228;  
    /*background-image: url(back.png);*/  
    display: flex;  
    flex-direction: column;  
    align-items: center; /* 수직 정렬 (가운데) */  
    justify-content: center; /* 수평 정렬 (가운데) */  
    height: 100%; /* 화면 높이에 맞추어 정렬 */    
    overflow: auto;  
}  
  
.status-img {  
    width: 250px;  
    height: 250px;  
}  
  
.view {  
    color: white;  
    font-size: 40px;  
}  
  
.status {  
    font-size: 110px;  
    font-weight: bold;  
}  
  
.text-occupancy {  
    vertical-align: top;  
    background-color: white;  
    padding: 7px;  
    color: #001228;  
    border-radius: 10px;  
    font-size: 70px;  
    width: 280px;  
    height: 160px;  
    font-weight: bold;  
}  
  
.text-max {  
    vertical-align: top;  
    background-color: white;  
    padding: 7px;  
    color: #001228;  
    border-radius: 10px;  
    font-size: 70px;  
    width: 280px;  
    height: 160px;  
    font-weight: bold;  
}  
  
.flex-container {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: row;  
    margin: 30px;  
}  
  
.flex-item1 {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: column;  
    margin-top: 20px;  
    height: 10vh;  
    padding-top: 50px;  
    margin-right: 30px;  
}  
  
.flex-item2 {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: column;  
    margin-top: 20px;  
    height: 10vh;  
    padding-top: 50px;  
}  
  
.time {  
    width: 38px;  
}  
  
.admin-color {  
    color: white;  
}  
  
.admin-container1 {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: column;  
    margin-left: 40px;  
}  
  
.admin-container2 {  
    display: flex;  
    justify-content: center;  
    align-items: center;  
    flex-direction: column;  
    margin-left: 40px;  
    padding: 40px;  
}
```