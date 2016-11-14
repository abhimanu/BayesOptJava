#mv output output4
rm -rf grid.pkl ExpectedImprovement.pkl  output/ jobs/ ExpectedImprovement_hyperparameters.txt 
#cat output5/00000* | grep "Got " | cut -d' ' -f3  | pbcopy
./BayesOptRunner.py 
