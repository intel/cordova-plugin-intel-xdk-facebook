/*
Copyright 2015 Intel Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file 
except in compliance with the License. You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the 
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific language governing permissions 
and limitations under the License
*/

function capturePhoto() {
    intel.xdk.camera.takePicture(100, false, "png");
}

function importPhoto() {
    intel.xdk.camera.importPicture();
}

function deletePhoto() {
    var arrPictureList = intel.xdk.camera.getPictureList();

    if (arrPictureList.length > 0) {
        intel.xdk.camera.deletePicture(arrPictureList[0]);
    }
}

function clearPhoto() {
    var arrPictureList = intel.xdk.camera.getPictureList();
    intel.xdk.camera.clearPictures(arrPictureList[0]);
}

function onRemove(evt) {
    removeImagesFromPage();
    addAllImagesToPage();

    if (evt.success == true) {
        alert(evt.filename + " has been removed from the application's picture list");
    }
   document.getElementById('totalPictureCount').innerHTML = intel.xdk.camera.getPictureList().length + " items in the picture list";
}
document.addEventListener("intel.xdk.camera.picture.remove", onRemove);

function onSuccess(evt) {
    if (evt.success == true) {
        removeImagesFromPage();
        addAllImagesToPage();
    }
    else {
        if (evt.message != undefined) {
            alert(evt.message);
        }
        else {
            alert("error capturing picture");
        }
    }
    document.getElementById('totalPictureCount').innerHTML = intel.xdk.camera.getPictureList().length + " items in the picture list";
}

document.addEventListener("intel.xdk.camera.picture.add", onSuccess);
document.addEventListener("intel.xdk.camera.picture.busy", onSuccess);
document.addEventListener("intel.xdk.camera.picture.cancel", onSuccess);

function onClear(evt) {
    removeImagesFromPage();
    addAllImagesToPage();

    if (evt.success == true) {
        alert("The picture list has been cleared");
    }
    document.getElementById('totalPictureCount').innerHTML = intel.xdk.camera.getPictureList().length + " items in the picture list";
}
document.addEventListener("intel.xdk.camera.picture.clear", onClear);

function removeImagesFromPage() {
    var images = document.getElementsByTagName('img');
    var l = images.length;
    for (var i = 0; i < l; i++) {
        images[0].parentNode.removeChild(images[0]);
    }
}

function addAllImagesToPage() {
    var arrPictureList = intel.xdk.camera.getPictureList();
    for (var x = 0; x < arrPictureList.length; x++) {
        // create image 
        var newImage = document.createElement('img');
        newImage.src = intel.xdk.camera.getPictureURL(arrPictureList[x]);
        newImage.setAttribute("style", "width:100px;height:100px;");
        newImage.id = document.getElementById("img_" + arrPictureList[x]);

        var photoHolder = document.getElementById("photoHolder");
        photoHolder.appendChild(newImage);
    }
}