Integrate Real Screener: Replace the static ticker list with a call to a screener endpoint (e.g., FMP Stock Screener).
Add Mock Indicators: Add a simple "Mock Mode" badge to the top of the Dashboard when providerFactory falls back to mock data.
Expand Test Coverage: Add UI tests (Compose Tests) to verify the Settings navigation and Alert Permission dialog interactions.

For the stock screener you can utilise whichever 2 providers offer the best functionality. Ensure they work across the stack and synthesising process. Also offer custom ticker entering for user querying, but advise they use the API and LLM suggested ones. Also ensure this addition is added to the llm/api transparency log implemented previously

You can decide a suitable design for mock mode

And add all tests required

First extend the alert thresholds section of settings to explain what each configuration is and include an ai explanation section for why the values were suggested based on the user's input.

Then please add an additional setting the stock screener tolerance so that users can tweak the conservative, moderate and aggressive thresholds. We should also offer an ask ai for suggestions for this, again providing an explanation for why the values were selected.

Please also ensure that the logviewerscreen doesn't reveal actual application logs, this should be a rendered and cleaned output from the api requests and llm requests, designed to show the inputs and outputs of every activity but not be a true log for android events.

Also, we need to offer customisation for the alert frequency so users can increase or decrease the 15 minute threshold, with clear messaging that smaller intervals will use api quotas more.

Next, we need to log and surface how many api requests have been completed within the current month to provide transparency for how close a user is to the thresholds and limits provided by our api providers.

Finally, can we extend the custom ticker input so it allows users to search for tickers instead of just entering the ticker value, we should offer a list addition rather than a comma separated input. We also need to update all areas of the UI that show ticker values to have a clear indicator they were added by the user in the settings and not suggested by the ai synthesis pipeline.