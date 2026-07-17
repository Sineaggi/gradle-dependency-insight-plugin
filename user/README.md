* [ ] dependency report task
* [ ] dependency report aggregation



dependencySizeAggregation ->
aggregateDependencySizeReportResults ->

will need variant awareness.

implementation(project(":list"))
implementation(project(":utilities"))

or 

dependencySizeAggregation(project(":application")) 
