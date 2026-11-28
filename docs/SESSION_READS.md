# Calendar-scoped session reads

Today and the launcher widgets no longer observe the full session history.
While the app is open, Today and both widgets share one observable read for
the current Monday–Sunday week. A widget updated independently reads just the
day or week it displays. Profile History and Study Insights still have access
to all sessions.

The query uses inclusive starts and exclusive ends derived from the device's
local day and time zone. It does not assume that every day is 24 hours.
The observed result carries its calendar period so that data loaded under one
period is not displayed under another. Resume and system clock, date, and
time-zone changes refresh the period.

Read boundaries, widget output parity, original session rows and notes, and live
Today updates are covered by Room and Compose tests. This is a read-only change
to the session store: the Room schema and version are unchanged.

A local synthetic SQLite check with 50,000 stored sessions returned 28 instead
of 50,000 rows for the same week. Fetching and materializing the results took
about 2 ms instead of 61 ms (median of nine reads on the development machine).
Both plans still scan the table and sort the result. This is not an Android
benchmark; a timestamp index and migration tests are separate work.

[Room observable queries](https://developer.android.com/training/data-storage/room/async-queries)
