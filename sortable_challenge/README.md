<h1> How to run the program </h1>

<p>
run.sh takes a command line parameter.
</p>

<ul>
<li>./run.sh clean -> removes the *.class file and results.txt file</li>
<li>./run.sh match -> compiles and runs the MatchProduct.java function</li>
</ul>

<h2> Notes on the thought process </h2>

<p> 
The requirement of this challenge was accuracy. In order to parse through the large
text file, initially the manufacturers are grouped from the listings.txt file so that 
the search can be made faster by going through a smaller set of the list. The data structure
that was used was a hashmap of Strings and a list of respective JSONObjects. This was chosen 
so that when every product was parsed from products.txt, it can start by looking up the 
manufacturer first from the hashmap. Then after some careful analysis, it was observed that the
model name and family always existed in the listing title. So these attributes were used
to exactly match with the listing title. If both of these existed, then a match is made and added
to the results.txt hashmap.
</p>

<p>
Optimization decisions that were made -
</p>

<ul>
	<li>Use hashmap for quick look ups and proper grouping of listing items</li>
	<li>Use an iterator to parse through large lists. Apparently this is more efficient as I was questioned about this in one of my interviews</li>
	<li>Created a smarter string matching algorithm. Match-off-by-one process matches the model or the family name where the strings to be matched
	are off by one character and that character is a dash. If this search process fails, a last attempt is made by replacing the spaces in the model or family 
	name with dashes and searching again. COMMENT: By using this algorithm, the hit rate has increased with accuracy and the algorithm runtime has decreased
	because we are now using the convention of searching based on the spaces provided in the listing title. </li>
</ul>

<p>Items left to do</p>
<ul>
	<li>Check for prices to remove duplicacy of matching. But all items are unique as they have different prices even if the title is the same. So to make the current algorithm efficient, this check is not done yet.</li>
</ul>
