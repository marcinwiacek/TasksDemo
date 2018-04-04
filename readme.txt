TASK:

"Screen 1: The main screen consists of 2 different views (tabs or something similar). The first tab
contains a list of tasks that need to be processed (Pending Tasks). All tasks can be executed concurrently.
The list should be "swipe enabled" where left swipe postpones the task for 1 min and
the right swipe starts the task immediately. The second tab shows all completed tasks with time
how long it took for completion. Swiping each of the rows in the second tab puts the task for execution
again. In both tabs, you need to add a way to add the new task or to edit the existing
tasks. Add/Edit will bring up the screen 2

Screen 2: Task creation screen, which contains a "Name" input field, "Description" input field
and a "file upload button" where the user can choose a file from the device or from the third-party
Apps. Also, this screen contains the list of "keyword" input field and a "+ add keyword" button
to adding more "keyword" fields to the keyword list dynamically. At the end, the task should upload
the file to cloud storage (Dropbox, iCloud or others) and all other fields should all be saved
in local storage. This screen contains buttons "Save" and "Cancel". Pressing the "Save" button
puts the task in the queue for execution (Pending Tasks) and closes the screen.
Also, create a notification mechanism that will inform the user of tasks status, if the task is still
executing it should show progress, and if it is done, it should show appropriate info. If multiple
tasks are done, show this accordingly. Pressing on a notification, should open the application on
Screen 1. Please add Cancel and New action buttons, where Cancel will stop the current execution
task, and New will open a Screen 2."

APPLICATION:

Application was tested with Android Studio 3.1 and Pixel / Android 8.1
and it shows some concepts:

1. using RecycleView, Adapter, DataSource and left / right swiping 
   for items (currently there is shown just color background when user 
   starts swipe)

2. running Activity, passing data with Intent and waiting for Result
   when running other Activity

3. using Broadcasts for notifying about updates (some are available
   when application is not running and some are created programmatically)

4. using system File Picker

5. creating Notifications with buttons and actions and progress
   (progress is updated using Job Service and because of it application
   needs Android 5.1)

6. JavaDocs (put in some places)

7. putting TODO in the code

8. FireBase Storage / Google authentication

MORE TODO:

1. using DataSource and probably SQLite instead of files for saving tasks
   (files code was short)

2. testing & adding JUnit & automatic tests

3. more JavaDocs

4. using ConstraintLayouts (performance)

5. more Android look & feel (looking very carefully into Google specs and following
   guidelines, providing more nice file picker part in Add/Edit window, closing "+"
   during editing in Add/Edit window, etc.)

6. notification should some progress, but... we don't know task length and right
   now we set max with modulo (progress is going to end and is starting from beginning)

7. adding support for AlarmManager to support older API

8. APK size optimalization

9. every notification is updated with separate Job Service (from performance/battery
   usage point of view we should probably have one Job Service only)

10. uploading files in background

COMPILING AND PREPARING:

1. import Gradle project in the Android Studio
2. create Storage in Firebase:
   1. https://console.firebase.google.com/
   2. Add project
   3. fill details and click Create Project
   4. Storage
   5. Get Started
   6. Accept default rules by clicking GOT IT
3. setup authentication:
   1. https://console.firebase.google.com/
   2. Open your project
   3. Authentication
   4. SIGN-IN METHOD
   5. enable your provider(s) like Google
4. download google-services.json and add it to the project in the app directory:
   1. https://console.firebase.google.com/
   2. Open your project
   3. Settings
   4. Project settings
   5. Add app (application package name: mwiacek.com.tasksdemo)
   6. Click google-services.json
5. enable Identify Toolkit API:
   1. take project_id from google-services.json and use URL below with "tasksdemo-96518" replaced with project_id:
      https://console.developers.google.com/apis/api/identitytoolkit.googleapis.com/overview?project=tasksdemo-96518

NOTES:

* application shows some concepts and coding level, but it's more exercise 
  than real product and some things were not done just because of time limits.

* application can be improved infinitely, with so many sources, 
  manuals, tutorials it's also rather matter of time than skills.

* used code style is as close as possible to this used for example in Chrome.

* don't be so serious! have fun! remember: many of us is thinking 
  about improving code just after writing it!

HISTORY:

4 (5.4)
* cleanup

3 (3.4)
* upgrading Gradle to 4.4 and checking with Android Studio 3.1
* adding uploading file to Firebase

2 (28.03)
* implemented displaying progress in notifications

1 (26.03)
* initial release
