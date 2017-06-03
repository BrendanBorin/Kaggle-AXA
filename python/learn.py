import csv
import pandas as pd
import numpy
import glob
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.ensemble import RandomForestRegressor
from sklearn.svm import SVC
from sklearn.ensemble import GradientBoostingClassifier

iterations = 1

submission = open('../new_submission.csv', 'w')
submission.write('driver_trip,prob\n')

#classifier = LogisticRegression(penalty='l2', dual=False, C=1)
classifier = RandomForestClassifier(n_estimators=500, max_depth=12, min_samples_leaf=5, random_state=53017)
#classifier = RandomForestRegressor(n_estimators=300, max_depth=5, random_state=53017)
#classifier = SVC()
#classifier = GradientBoostingClassifier(n_estimators=50, max_depth=2, min_samples_leaf=5, random_state=53017)

misclassed = 0

for file in glob.glob('..\\drivers\\features\\*sup.csv'):
    #filename = 'drivers/features/1_features_sup.csv'
    trainfile = pd.read_csv(file)
    #print(trainfile.head())
    driver = file.split('\\')[3].split('_')[0]
    train = trainfile.ix[:, 2:-1].values
    labels = trainfile['label'].values

    for i in range(iterations):
        classifier.fit(train, labels)
        pred = classifier.predict(train)
        print(sum(pred[0:200]))
        if i == iterations - 1:
            misclassed = misclassed + sum(pred[200:400])
        pred[200:400] = 0
        labels = pred

    #print classifier.feature_importances_
    probs = classifier.predict_proba(train)
    #print probs

    #calculate number of misclassified negatives

    for i in range(200):
        trip = str(trainfile.iloc[i]['drive']).split('.')[0]
        driver_trip = driver + '_' + trip
        prob = round(probs[i, 1], 3)
        submission.write(driver_trip + ',' + str(prob) + '\n')

submission.close()
print('misclassed: ' + str(misclassed))
