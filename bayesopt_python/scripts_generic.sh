#mv output output4
rm -rf grid.pkl ExpectedImprovementGeneric.pkl  output/ jobs/ ExpectedImprovementGeneric_hyperparameters.txt 
#cat output5/00000* | grep "Got " | cut -d' ' -f3  | pbcopy
python BayesOptRunnerGeneric.py --putprior 
