Flurry code:
Config.FLURRY_KEY

In each activity:

public void onStart()
{
   super.onStart();
   FlurryAgent.onStartSession(this, Config.FLURRY_KEY);
   // your code
}

public void onStop()
{
   super.onStop();
   FlurryAgent.onEndSession(this);
   // your code
}